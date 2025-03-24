export interface Metadata {
    title?: string,
    isbn10?:string,
    isbn13?: string,
    summary?: string,
    image?: string,
    publisher?: string,
    pageCount?: number,
    publishedDate?: string,
    authors: Array<string>,
    tags: Array<string>,
    series?: string,
    numberInSeries?: number,
    language?: string,
    googleId?: string,
    amazonId?: string,
    goodreadsId?: string,
    librarythingId?: string,
    isfdbId?: string,
    openlibraryId?: string,
    noosfereId?: string,
    inventaireId?: string,
}
