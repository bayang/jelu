export interface Review {
    id: string,
    creationDate: string,
    modificationDate: Date,
    reviewDate: Date,
    text: string,
    rating: number,
    user: string,
    book: string,
    visibility: Visibility
}

export interface CreateReviewDto {
    reviewDate?: Date,
    text: string,
    rating: number,
    bookId: string,
    visibility: Visibility
}

export interface UpdateReviewDto {
    reviewDate?: Date,
    text?: string,
    rating?: number,
    visibility?: Visibility
}

export enum Visibility {
    PUBLIC = 'PUBLIC',
    PRIVATE = 'PRIVATE'
}