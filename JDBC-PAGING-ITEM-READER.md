# JdbcPagingItemReader 내부 동작 분석

> `spring-batch-infrastructure 6.0.2` 소스(`JdbcPagingItemReader`, `AbstractPagingItemReader`,
> `AbstractItemCountingItemStreamItemReader`, `AbstractSqlPagingQueryProvider`,
> `SqlPagingQueryUtils`, `H2PagingQueryProvider`, `JdbcPagingItemReaderBuilder`)를 직접 읽고
> 이 프로젝트의 ex17 실행 로그로 실증한 내용입니다.
>
> **핵심 결론**: `JdbcPagingItemReader`는 OFFSET 기반이 아니라 **키셋(seek) 기반 페이징**입니다.
> `JpaPagingItemReader`(ex18)와는 내부 동작이 완전히 다릅니다.

## 1. 클래스 계층

```
AbstractItemCountingItemStreamItemReader<T>   ← read.count 기반 재시작 프레임워크(공통)
        │
AbstractPagingItemReader<T>                    ← "페이지 단위로 읽는다"는 공통 골격
        │
JdbcPagingItemReader<T>                        ← JDBC + PagingQueryProvider로 실제 SQL 실행
```

## 2. `doRead()` — 페이지를 언제 새로 가져오는가

```java
// AbstractPagingItemReader
protected T doRead() throws Exception {
    if (results == null || current >= pageSize) {
        doReadPage();      // 페이지 새로 가져오기
        page++;
        if (current >= pageSize) current = 0;
    }
    int next = current++;
    return next < results.size() ? results.get(next) : null;
}
```

`results`(현재 페이지의 행들)를 메모리에 들고 있다가 `current`가 `pageSize`를 넘어서면 그제서야
`doReadPage()`로 다음 페이지를 새로 조회합니다. 즉 **DB 왕복은 페이지 경계에서만** 일어나고,
그 사이 `read()` 호출은 메모리에서 처리됩니다.

## 3. `doReadPage()` — 실제 SQL 실행 (핵심)

```java
// JdbcPagingItemReader
protected void doReadPage() {
    if (getPage() == 0) {
        query = ...query(firstPageSql, ...);                    // 최초 1회만
    }
    else if (startAfterValues != null) {
        query = ...query(remainingPagesSql, getParameterMap(parameterValues, startAfterValues), ...);
    }
    results.addAll(query);
}
```

`getPage() == 0`(최초 페이지)이냐 아니냐로만 분기하지, **`page` 번호를 OFFSET 계산에 쓰지
않습니다.** 대신 `startAfterValues` — 직전 페이지 **마지막 행의 정렬 키(sort key) 값** — 를
파라미터로 넘겨서 "그 값 이후"를 조회합니다.

`remainingPagesSql`이 실제로 어떻게 생겼는지는 `SqlPagingQueryUtils.buildWhereClause(remainingPageQuery=true)`가
만듭니다:

```java
sql.append(" WHERE ");
if (원래 whereClause 있으면) sql.append("(" + whereClause + ") AND ");
buildSortConditions(...);   // (sortKey > :_sortKey) 형태 추가
sql.append(" ORDER BY ").append(정렬키...);
sql.append(limitClause);    // H2는 "FETCH NEXT N ROWS ONLY"
```

### 이 프로젝트(ex17)에서 실제로 찍힌 SQL

```sql
SELECT id, customer_id, order_datetime AS orderDateTime, status, shipping_id AS shippingId
FROM orders
WHERE ((status = 'SHIPPED' AND shipping_id IS NULL) OR (status = 'CANCELLED' AND shipping_id IS NOT NULL))
  AND ((id > ?))
ORDER BY id ASC
FETCH NEXT 10 ROWS ONLY
```

`OFFSET`이 어디에도 없습니다. `id > ?`(마지막으로 본 id) + `ORDER BY id` + `FETCH NEXT`만으로
다음 페이지를 가져옵니다. 이게 **키셋/seek 페이징**입니다. 클래스 Javadoc에도 명시돼 있습니다:

> "On restart, it uses the last sort key value to locate the first page to read (so it doesn't
> matter if the successfully processed items have been removed or modified)."

## 4. `startAfterValues`는 어디서 갱신되는가 — `PagingRowMapper`의 이중 역할

```java
private class PagingRowMapper implements RowMapper<T> {
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        startAfterValues = new LinkedHashMap<>();
        for (Map.Entry<String, Order> sortKey : queryProvider.getSortKeys().entrySet()) {
            startAfterValues.put(sortKey.getKey(), rs.getObject(sortKey.getKey()));
        }
        return rowMapper.mapRow(rs, rowNum);   // 실제 매핑은 사용자가 준 rowMapper에 위임
    }
}
```

행을 매핑할 때마다(페이지의 매 행마다) `startAfterValues`를 그 행의 정렬 키 값으로 계속
덮어씁니다. 그래서 페이지 조회가 끝나는 시점엔 자연스럽게 **"이 페이지 마지막 행의 키"**가
남아 있게 됩니다. 이게 다음 페이지 조회에 쓰이는 시크 키입니다.

## 5. 재시작(restart) — 두 가지 상태가 함께 동작

`ExecutionContext`엔 두 종류의 키가 저장됩니다.

- `[name].read.count` — 정확히 몇 번째 아이템까지 읽었는지 (공통 프레임워크,
  `AbstractItemCountingItemStreamItemReader`)
- `[name].start.after` — 마지막 페이지 조회에 쓴 시크 키 값 (`JdbcPagingItemReader` 전용)

재시작 시 `open()`에서:

```java
// JdbcPagingItemReader.open()
startAfterValues = executionContext.get([name].start.after);   // 시크 키 복원
super.open(executionContext);   // 이 안에서 jumpToItem(itemCount) 호출됨
```

```java
// AbstractPagingItemReader.jumpToItem()
page = itemIndex / pageSize;
current = itemIndex % pageSize;
```

즉 **"어느 페이지를 다시 가져올지"는 `startAfterValues`(시크 키)가 결정**하고, **"그 페이지에서
몇 번째 행부터 리턴할지"는 `current`(= itemIndex % pageSize)가 결정**합니다. 둘이 협업하는
구조입니다 — DB 조회는 시크 키로 정확한 지점부터, 메모리 스킵은 인덱스로.

### 왜 페이지 경계에서 멈췄냐 아니냐에 따라 저장하는 값이 다른가

```java
public void update(ExecutionContext executionContext) {
    if (isAtEndOfPage() && startAfterValues != null) {
        executionContext.put(START_AFTER_VALUE, startAfterValues);        // 이 페이지 마지막 키
    }
    else if (previousStartAfterValues != null) {
        executionContext.put(START_AFTER_VALUE, previousStartAfterValues); // 이전 페이지 마지막 키(=이 페이지 시작점)
    }
}
```

- 청크 커밋 시점이 **딱 페이지 끝과 맞아떨어지면** → 방금 다 읽은 페이지의 마지막 키를 저장 →
  재시작하면 **다음 페이지부터** 시크.
- 청크 커밋이 **페이지 중간**에서 일어나면(pageSize와 chunk size가 안 맞는 경우) → 대신
  `previousStartAfterValues`(현재 페이지의 시작점, 즉 이전 페이지의 마지막 키)를 저장 →
  재시작하면 **현재 페이지를 처음부터 통째로 다시 fetch**하고, `current`
  (= itemCount % pageSize)만큼 메모리에서 건너뜁니다.

굳이 중간 지점의 정확한 키를 저장하지 않고 "페이지 시작 + 인덱스 스킵" 조합으로 처리하는 게,
중간 행의 키를 별도로 추적하는 것보다 단순하고 안전하기 때문으로 보입니다.

## 6. 초기화 시점(`afterPropertiesSet`)에 이미 SQL이 확정됨

```java
public void afterPropertiesSet() throws Exception {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.setMaxRows(getPageSize());        // 방어적으로 한 번에 pageSize개까지만
    queryProvider.init(dataSource);
    this.firstPageSql = queryProvider.generateFirstPageQuery(getPageSize());
    this.remainingPagesSql = queryProvider.generateRemainingPagesQuery(getPageSize());
}
```

두 SQL 문자열은 **빈 초기화 시점에 딱 한 번만 만들어지고 재사용**됩니다. 매 페이지 조회마다
SQL을 다시 조립하지 않습니다.

## 7. DB별 SQL은 어떻게 정해지는가 — 자동 감지

이 프로젝트의 ex16/ex17처럼 `.queryProvider(...)`를 직접 안 주고 `selectClause`/`fromClause`/
`sortKeys`만 준 경우:

```java
// JdbcPagingItemReaderBuilder.determineQueryProvider()
DatabaseType databaseType = DatabaseType.fromMetaData(dataSource);  // JDBC 메타데이터로 DB 제품명 확인
// 그에 맞는 구현체 선택: H2PagingQueryProvider, MySqlPagingQueryProvider, OraclePagingQueryProvider ...
```

H2를 쓰는 이 프로젝트는 `H2PagingQueryProvider`가 선택되고, 이건 `FETCH NEXT N ROWS ONLY`
문법을 씁니다. DB마다 페이징 문법이 다르니(`LIMIT`, `TOP`, `ROWNUM` 등) provider 클래스가
DB별로 따로 존재합니다(`MySqlPagingQueryProvider`, `OraclePagingQueryProvider`,
`SqlServerPagingQueryProvider`, `PostgresPagingQueryProvider` 등).

## 8. `JpaPagingItemReader`(ex18)와의 결정적 차이

ex18의 `JpaPagingItemReader`를 돌렸을 때 찍힌 실제 Hibernate SQL:

```sql
... order by p1_0.id offset ? rows fetch first ? rows only
```

**`OFFSET`이 있습니다.** 즉 `JpaPagingItemReader`는 진짜 OFFSET 기반이라 "데이터가 페이징
도중 추가/삭제되면 항목을 건너뛰거나 중복 읽을 수 있다"는 단점이 실제로 적용됩니다. 반면
`JdbcPagingItemReader`는 시크 키 기반이라 그 문제로부터 자유롭습니다.

| | JdbcPagingItemReader | JpaPagingItemReader |
|---|---|---|
| 페이징 방식 | 키셋/seek (`WHERE sortKey > ?`) | OFFSET (`OFFSET ? ROWS FETCH NEXT ? ROWS ONLY`) |
| 동시 데이터 변경에 대한 안전성 | 안전 (이미 지나간 행이 삭제/수정돼도 영향 없음) | 취약 (OFFSET이 밀려서 항목 스킵/중복 가능) |
| 재시작 시 사용하는 상태 | `read.count` + `start.after`(시크 키) | `read.count`만 (페이지 번호로 OFFSET 재계산) |
| 관련 문서 | 이 문서 | `BATCH-RETRY-SKIP-RESTART.md` 계열에서 다룬 "JpaPagingItemReader 사용 시 주의점" |

같은 "Paging" reader라는 이름이라도 구현에 따라 내부 동작이 완전히 다르므로, 문서/블로그의
"페이징 리더는 OFFSET이라 위험하다"는 일반론을 그대로 `JdbcPagingItemReader`에 적용하면
안 됩니다.

## 9. 실무에서 알아야 할 함정

1. **`sortKeys`는 반드시 유니크해야 함**: 시크 페이징이 정확하려면 정렬 키로 각 행을
   유일하게 식별할 수 있어야 합니다. 유니크하지 않은 컬럼을 sortKey로 쓰면, 같은 키 값을
   가진 행이 여러 개일 때 일부가 건너뛰어지거나 중복 조회될 수 있습니다. (ex16/ex17은
   PK인 `id`를 써서 안전합니다.)
2. **재시작 사이에 `pageSize`를 바꾸면 위험**: `current = itemIndex % pageSize`가 "새
   pageSize" 기준으로 계산되는데, 시크로 가져온 페이지의 실제 크기와 안 맞으면 스킵
   개수가 어긋나서 항목을 잘못 건너뛸 수 있습니다.
3. **sortKey 컬럼엔 인덱스가 사실상 필수**: `whereClause`와 결합된 `sortKey > ?` 조건이
   매 페이지마다 실행되므로, 인덱스가 없으면 페이지 조회가 매번 풀스캔이 될 수 있습니다.
4. **`selectClause`에 sortKey 컬럼이 포함돼야 함**: `PagingRowMapper`가 `rs.getObject(sortKey)`로
   값을 읽어야 하므로, 정렬 키로 쓰는 컬럼은 SELECT 목록에 반드시 있어야 합니다(별칭을
   써도 무방하되, sortKey 이름과 ResultSet 컬럼 라벨이 일치해야 함).

## 참고 소스 위치 (spring-batch-infrastructure 6.0.2)

- `org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader`
- `org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader`
- `org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader`
- `org.springframework.batch.infrastructure.item.database.support.AbstractSqlPagingQueryProvider`
- `org.springframework.batch.infrastructure.item.database.support.SqlPagingQueryUtils`
- `org.springframework.batch.infrastructure.item.database.support.H2PagingQueryProvider`
- `org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder`
