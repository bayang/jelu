import { Book, UserBook } from "../model/Book";
import Swal from 'sweetalert2';

export class ObjectUtils {

  // from https://javascript.plainenglish.io/deep-clone-an-object-and-preserve-its-type-with-typescript-d488c35e5574
  public static deepCopy<T>(source: T): T {
    return Array.isArray(source)
    //@ts-ignore
    ? source.map(item => this.deepCopy(item))
    : source instanceof Date
    //@ts-ignore
    ? new Date(source.getTime())
        : source && typeof source === 'object'
          ? Object.getOwnPropertyNames(source).reduce((o, prop) => {
            Object.defineProperty(o, prop, Object.getOwnPropertyDescriptor(source, prop)!);
            o[prop] = this.deepCopy((source as { [key: string]: any })[prop]);
            return o;
          }, Object.create(Object.getPrototypeOf(source)))
          : source as T;
  }

  public static toast = (oruga: any, variant: string, message: string, duration = 2000) => {
    oruga.notification.open({
      message: message,
      position: "top",
      variant: variant,
      duration: duration,
    });
  };

  public static toUserBook = (book: Book): UserBook => {
    const converted = {
      id: book.userBookId != null ? book.userBookId : null,
      book: book,
      lastReadingEvent: book.userbook?.lastReadingEvent != null ? book.userbook?.lastReadingEvent : null,
      lastReadingEventDate: book.userbook?.lastReadingEventDate != null ? book.userbook?.lastReadingEventDate : null,
      creationDate: book.userbook?.creationDate != null ? book.userbook?.creationDate : null,
      modificationDate: book.userbook?.modificationDate != null ? book.userbook?.modificationDate : null,
      personalNotes: book.userbook?.personalNotes != null ? book.userbook?.personalNotes : null,
      owned: book.userbook?.owned != null ? book.userbook?.owned : null,
      borrowed: book.userbook?.borrowed != null ? book.userbook?.borrowed : null,
      toRead: book.userbook?.toRead != null ? book.userbook?.toRead : null,
      readingEvents: book.userbook?.readingEvents != null ? book.userbook?.readingEvents : null,
      percentRead: book.userbook?.percentRead != null ? book.userbook?.percentRead : null,
      currentPageNumber: book.userbook?.currentPageNumber != null ? book.userbook?.currentPageNumber : null,
      avgRating: book.userbook?.avgRating != null ? book.userbook?.avgRating : null,
      userAvgRating: book.userbook?.userAvgRating != null ? book.userbook?.userAvgRating : null
    } as UserBook
    // console.log('after')
    // console.log(converted)
    return converted
  }

  public static unwrapUserBook = (book: Book): UserBook => {
    const userbook = book.userbook
    if (userbook != undefined) {
      userbook.book = {
        ...book,
        userbook: undefined
      }
      console.log('ub')
      console.log(userbook)
      return userbook
    } else {
      const converted = {
        id: undefined,
        book: book
      } as UserBook
      return converted
    }
  }
  public static swalMixin = Swal.mixin({
    background: '#404040',
    color: '#ffffff',
    buttonsStyling: false,
    customClass: {
      container: '',
      htmlContainer: 'mb-3 ml-3',
      popup: 'p-2 shadow-lg sm:p-3 bg-red-600',
      title: 'ml-4 truncate',
      input: 'bg-white text-center',
      confirmButton: 'btn btn-warning font-bold border-b-4 rounded-sm mx-1',
      cancelButton: 'btn btn-info font-bold border-b-4 rounded-sm mx-1',
      denyButton: 'btn btn-error font-bold rounded-sm mx-1'
    }
  })

  public static baseSwalMixin = Swal.mixin({
    background: '#404040',
    color: '#ffffff',
  })

  /**
   * 
   * @param newVals [currentPageNumber, percentRead, pageCount]
   * @param oldVals [currentPageNumber, percentRead, pageCount]
   * @param target userbook, or reactive form
   * @param pageCount current pageCount
   */
  public static computePages = (newVals: Array<number | string | null | undefined>, oldVals: Array<number | string | null | undefined>,
    target: { currentPageNumber?: number | null, percentRead?: number | null }, pageCount: number | null) => {
    console.log("pagecount " + pageCount)
    if (pageCount != null) {
      console.log(newVals)
      console.log(oldVals)
      if (newVals[0] as number >= pageCount) {
        target.currentPageNumber = pageCount
        target.percentRead = 100
      } else {
        target.percentRead = Math.min(100, ((newVals[0] as number * 100) / pageCount))
      }
    } else {
      target.currentPageNumber = null
    }
  }

  // https://stackoverflow.com/questions/39924644/es6-generate-an-array-of-numbers
  public static range = (start: number, end: number, step: number) => {
    return Array.from(Array.from(Array(Math.ceil((end - start) / step)).keys()), x => start + x * step);
  }
}
