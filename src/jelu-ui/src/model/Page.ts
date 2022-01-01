interface Page<T> {
    content: T[],
    pageable: Pageable,
    last: boolean,
    totalPages: number,
    totalElements: number,
    first: boolean,
    size: number,
    number: number,
    sort: Sort,
    numberOfElements: number,
    empty: boolean
}

interface Pageable {
    sort: Sort,
    pageNumber: number,
    pageSize: number,
    offset: number,
    paged: boolean,
    unpaged: boolean,
}

interface Sort {
    sorted: boolean,
    empty: boolean,
    unsorted: boolean,
}
