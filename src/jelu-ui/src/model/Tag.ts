import { BookWithUserBook } from "./Book";

export interface Tag {
    id?: string,
    creationDate?: string,
    name: string,
    modificationDate?: string,
  }
  export interface TagWithBooks {
    id?: string,
    creationDate?: string,
    name: string,
    modificationDate?: string,
    books: Array<BookWithUserBook>
  }
  