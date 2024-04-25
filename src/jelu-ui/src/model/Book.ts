import {Author} from "./Author";
import { ReadingEvent, ReadingEventType } from "./ReadingEvent";
import { SeriesOrder } from "./Series";
import { Tag } from "./Tag";

export interface Book {
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
    translators?: Array<Author>,
    tags?: Array<Tag>,
    series?: Array<SeriesOrder>, 
    googleId?: string,
    amazonId?: string,
    goodreadsId?: string,
    librarythingId?: string,
    language?: string,
    userBookId?: string,
    userbook?: UserBook,
  }
export interface UserBook {
    id?: string,
    lastReadingEvent?: ReadingEventType|null,
    lastReadingEventDate?: string,
    creationDate?: string,
    modificationDate?: string,
    personalNotes?: string,
    owned?: boolean|null,
    borrowed?: boolean|null,
    toRead?: boolean|null,
    book: Book,
    readingEvents?: Array<ReadingEvent>|null,
    percentRead? : number|null,
    currentPageNumber?: number|null,
    avgRating?: number|null,
    userAvgRating?: number|null,
  }
export interface UserBookBulkUpdate {
    ids: Array<string>,
    toRead?: boolean,
    owned?: boolean,
    removeTags?: Array<string>,
    addTags?: Array<string>,
}
export interface UserBookUpdate {
  id?: string,
  lastReadingEvent?: ReadingEventType|null,
  lastReadingEventDate?: string,
  creationDate?: string,
  modificationDate?: string,
  personalNotes?: string,
  owned?: boolean|null,
  borrowed?: boolean|null,
  toRead?: boolean|null,
  book?: Book,
  readingEvents?: Array<ReadingEvent>|null,
  percentRead? : number|null,
  currentPageNumber?: number|null,
}