import { UserBook } from "./Book";

export interface ReadingEvent {
  id?: string,
  creationDate?: string,
  modificationDate?: Date,
  eventType: ReadingEventType,
}

export interface CreateReadingEvent {
  eventDate?: Date,
  bookId?: string,
  eventType: ReadingEventType,
}

export interface ReadingEventWithUserBook {
  id?: string,
  creationDate?: string,
  modificationDate?: Date,
  eventType: ReadingEventType,
  userBook: UserBook
}

export enum ReadingEventType {
  FINISHED = 'FINISHED',
  DROPPED = 'DROPPED',
  CURRENTLY_READING = 'CURRENTLY_READING',
  NONE = 'NONE'
}

