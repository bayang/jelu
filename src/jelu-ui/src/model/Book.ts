import Author from "./Author";
import ReadingEvent from "./ReadingEvent";

export default interface Book {
    id?: string,
    creationDate?: string,
    title: string,
    isbn10?:string,
    isbn13?: string,
    summary?: string,
    publisher?: string,
    pageCount?: number,
    publishedDate?: string,
    modificationDate?: string,
    authors?: Array<Author>,
    readingEvents?: Array<ReadingEvent>
  }
  