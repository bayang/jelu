export interface SeriesOrder {
    seriesId?: string,
    name: string,
    numberInSeries? : number|null,
}
export interface Series {
    id?: string,
    creationDate?: string,
    name: string,
    modificationDate?: string,
    avgRating?: number,
    userRating?: number,
    description?: string,
}
export interface SeriesUpdate {
    name?: string,
    description?: string,
    rating?: number,
}
