export interface YearStats {
    dropped: number,
    finished: number,
    year: number,
    pageCount: number,
    price: number,
}

export interface MonthStats {
    dropped: number,
    finished: number,
    year: number,
    month: number,
    pageCount: number,
    price: number,
}

export interface TotalsStats {
    read: number,
    unread: number,
    dropped: number,
    total: number,
    price: number,
}
