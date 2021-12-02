import Author from "./Author";
import ReadingEvent, { ReadingEventType } from "./ReadingEvent";

export default interface Book {
    id?: string,
    creationDate?: string,
    title: string,
    isbn10?:string,
    isbn13?: string,
    summary?: string,
    publisher?: string,
    image?: string|null,
    pageCount?: number|null,
    publishedDate?: string|null,
    modificationDate?: string,
    authors?: Array<Author>,
    readingEvents?: Array<ReadingEvent>,
    readingEvent?: ReadingEventType|null,
  }
  