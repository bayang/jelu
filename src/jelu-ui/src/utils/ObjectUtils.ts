import { BookWithUserBook, UserBook } from "../model/Book";
import Swal from 'sweetalert2';

export class ObjectUtils {

  // from https://javascript.plainenglish.io/deep-clone-an-object-and-preserve-its-type-with-typescript-d488c35e5574
  public static deepCopy<T>(source: T): T {
    return Array.isArray(source)
      ? source.map(item => this.deepCopy(item))
      : source instanceof Date
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

  public static toUserBook = (book: BookWithUserBook):UserBook => {
    const {userBooks, ...rest} = book
      if (userBooks && userBooks.length > 0) {
        const converted =  {
          book: rest,
          ...userBooks[0]
        } as UserBook
        // console.log('after full')
        // console.log(converted)
        return converted
      }
      else {
        const converted =  {
          book: rest
        } as UserBook
        // console.log('after')
        // console.log(converted)
        return converted
      }
  }

  public static swalMixin = Swal.mixin({
    background: '#404040',
    color: '#ffffff',
  })
  
}