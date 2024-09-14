import { Visibility } from "./Review"

export interface BookQuote {
    id: string,
    creationDate: string,
    modificationDate: Date,
    text: string,
    user: string,
    book: string,
    visibility: Visibility,
    position?: string
}

export interface CreateBookQuoteDto {
    text: string,
    bookId: string,
    visibility: Visibility,
    position?: string
}

export interface UpdateBookQuoteDto {
    text?: string,
    visibility?: Visibility,
    position?: string
}
