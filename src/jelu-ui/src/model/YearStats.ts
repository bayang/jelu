export interface YearStats {
    dropped: number,
    finished: number,
    year: number,
    pageCount: number,
    priceInCents: number,
}

export interface MonthStats {
    dropped: number,
    finished: number,
    year: number,
    month: number,
    pageCount: number,
    priceInCents: number,
}

export interface TotalsStats {
    read: number,
    unread: number,
    dropped: number,
    total: number,
    priceInCents: number,
}
