import axios, { AxiosError, AxiosInstance } from "axios";
import { UserBook, Book, BookWithUserBook } from "../model/Book";
import { Author } from "../model/Author";
import router from '../router'
import { User, UserAuthentication } from "../model/User";
import { ReadingEventType, ReadingEventWithUserBook } from "../model/ReadingEvent";
import { Tag } from "../model/Tag";
import { Metadata } from "../model/Metadata";
import { Page } from "../model/Page";
import { Quote } from "../model/Quote";
import { ServerSettings } from "../model/ServerSettings";
import { ImportConfigurationDto } from "../model/ImportConfiguration";

class DataService {

  private apiClient: AxiosInstance;
  
  private token?: string = '';

  private TOKEN_KEY = 'jelu-token'

  private API_BOOK = '/books';

  private API_USERBOOK = '/userbooks';

  private API_AUTHOR = '/authors';

  private API_TAG = '/tags';

  private API_LOGOUT = '/logout';

  private API_METADATA = '/metadata';

  private API_QUOTES = '/quotes';

  private API_READING_EVENTS = '/reading-events';

  private API_SERVER_SETTINGS = '/server-settings';

  private API_IMPORTS = '/imports';

  private MODE: string;

  private BASE_URL: string;
  
  constructor() {
    if (import.meta.env.DEV) {
      this.MODE = "dev"
      this.BASE_URL = import.meta.env.VITE_API_URL as string
    }
    else {
      this.MODE = "prod"
      this.BASE_URL = window.location.origin
      this.BASE_URL.endsWith("/") ? this.BASE_URL = this.BASE_URL + "api/v1" 
      : this.BASE_URL = this.BASE_URL + "/api/v1"
    }
    console.log(`running in ${this.MODE} mode at ${this.BASE_URL}`)

    this.apiClient = axios.create({
      baseURL: this.BASE_URL,
      headers: {
        "Content-type": "application/json",
        'X-Requested-With': 'XMLHttpRequest'
      },
      withCredentials: true,
    });
    
    this.apiClient.interceptors.request.use((config) => {
      const tok = this.getToken()
      if (tok != null) {
        console.log('interceptor token ' + tok)
        if (!config.headers) {
          config.headers = {}
        }
        config.headers["X-Auth-Token"] = tok;
      }
      // Do something before request is sent
      return config;
    }, function (error) {
      // Do something with request error
      return Promise.reject(error);
    });
    this.apiClient.interceptors.response.use(
      originalResponse => {
        return originalResponse;
    },
    error => {
      console.log(`response error interceptor ${error.response.status}`)
      if (error.response.status === 401) {
        router.push({name: 'login'}).then(() => {console.log("ok nav in interceptor")}).catch(() => {console.log("error nav in interceptor")})
      }
    });
  }
  
  getToken = ():string | null => {
    if (this.token != null && this.token.trim().length > 0) {
      console.log('get tok from property')
      localStorage.setItem(this.TOKEN_KEY, this.token)
      return this.token
    }
    else if (localStorage.getItem(this.TOKEN_KEY) != null) {
      console.log('get tok from storage')
      return localStorage.getItem(this.TOKEN_KEY)
    }
    else {
      return null
    }
  }

  findUserBooks = async (page?: number, pageSize?: number) => {
    try {
      const response = await this.apiClient.get<Page<UserBook>>(`${this.API_USERBOOK}/me`, {
        params: {
          page: page,
          pageSize: pageSize
        }
      });
      console.log("called backend")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error findall " + (error as AxiosError).code)
      throw new Error("error findall " + error)
    }
  }

  getUserBookById = async (userBookId: string) => {
    try {
      const response = await this.apiClient.get<UserBook>(`${this.API_USERBOOK}/${userBookId}`);
      console.log("called userBook " + userBookId)
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error findall " + (error as AxiosError).code)
      throw new Error("error finding userBook " + userBookId + " " + error)
    }
  }

  getUser = async () => {
    try {
      const response = await this.apiClient.get<UserAuthentication>('/users/me')
      console.log("called user")
      console.log(response.data)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios user " + error.response.status + " " + error.response.data.error)
      }
      console.log("error user " + (error as AxiosError).code)
      throw new Error("error user " + error)
    }
  }

  authenticateUser = async (login: string, password: string) => {
    try {
      const response = await this.apiClient.get<UserAuthentication>('/users/me', {
        auth: {
          username: login,
          password: password,
        },
      })
      console.log("called user")
      console.log(response.data)
      console.log(response.data.token)
      if (response.data.token != null && response.data.token.length > 0) {
        this.token = response.data.token
        localStorage.setItem("jelu-token", this.token)
      }
      return response.data.user

    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios auth user " + error.response.status + " " + error.response.data.error)
        throw new Error("login error " + error.response.status + " " + error)
      }
      console.log("error auth user " + (error as AxiosError).code)
      throw new Error("login error " + error)
    }
  }

  fetchToken = async (login?: string, password?: string) => {
    try {
      let response;
      if (login != null && password != null 
        && login.trim().length > 0 && password.trim().length > 0) {
          response = await this.apiClient.get('/token', {
            auth: {
              username: login,
              password: password,
            },
          })
      }
      else {
        response = await this.apiClient.get('/token')
      }
      console.log("called auth token")
      console.log(response.data.token)

      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios auth token " + error.response.status + " " + error.response.data.error)
      }
      console.log("error auth token " + (error as AxiosError).code)
      throw new Error("error auth token " + error)
    }
  }

  setupStatus = async () => {
    try {
      const response = await this.apiClient.get('/setup/status')
      console.log(`setup resp `)
      console.log(response.data)
      return response.data.isInitialSetup
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios setup " + error.response.status + " " + error.response.data.error)
      }
      console.log("error setup " + (error as AxiosError).code)
      throw new Error("error setup " + error)
    }
  }

  createUser = async (login: string, password: string) => {
    try {
      const resp = await this.apiClient.post<User>('/users', {
          'login' : login,
          'password' : password, 
          'isAdmin' : true
      },
      {
        auth: {
          username: 'setup',
          password: 'initial',
        },
      })
      console.log('create user ')
      console.log(resp.data)
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error create user " + error.response.status + " " + error.response.data.error)
        throw new Error("error create user " + error.response.status + " " + error)
      }
      console.log("error create user " + (error as AxiosError).code)
      throw new Error("error create user " + error)
    }
  }

  saveBook = async (book: Book) => {
    try {
      const resp = await this.apiClient.post<Book>(this.API_BOOK, book)
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error saving book " + error.response.status + " " + error.response.data.error)
        throw new Error("error saving book " + error.response.status + " " + error)
      }
      console.log("error saving book " + (error as AxiosError).code)
      throw new Error("error saving book " + error)
    }
  }

  saveBookImage = async (book: Book, file: File|null, onUploadProgress:any) => {
    try {
      const formData = new FormData()
      if (file != null) {
        formData.append('file', file);
      }
      formData.append('book', new Blob([JSON.stringify(book)], {
        type: "application/json"
    }));
      const resp = await this.apiClient.post<Book>(this.API_BOOK, formData,
       { 
        headers:{
          'Content-Type':'multipart/form-data',
          'Accept':'application/json'
        }, 
        onUploadProgress: onUploadProgress})
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error saving book " + error.response.status + " " + error.response.data.error)
        throw new Error("error saving book " + error.response.status + " " + error)
      }
      console.log("error saving book " + (error as AxiosError).code)
      throw new Error("error saving book " + error)
    }
  }

  saveUserBookImage = async (userBook: UserBook, file: File|null, onUploadProgress:any) => {
    try {
      const formData = new FormData()
      if (file != null) {
        formData.append('file', file);
      }
      formData.append('book', new Blob([JSON.stringify(userBook)], {
        type: "application/json"
    }));
      const resp = await this.apiClient.post<UserBook>(this.API_USERBOOK, formData,
       { 
        headers:{
          'Content-Type':'multipart/form-data',
          'Accept':'application/json'
        }, 
        onUploadProgress: onUploadProgress})
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error saving book " + error.response.status + " " + error.response.data.error)
        throw new Error("error saving book " + error.response.status + " " + error)
      }
      console.log("error saving book " + (error as AxiosError).code)
      throw new Error("error saving book " + error)
    }
  }

  updateUserBookImage = async (userBook: UserBook, file: File|null, onUploadProgress:any) => {
    try {
      const formData = new FormData()
      if (file != null) {
        formData.append('file', file);
      }
      formData.append('book', new Blob([JSON.stringify(userBook)], {
        type: "application/json"
    }));
      const resp = await this.apiClient.put<UserBook>(`${this.API_USERBOOK}/${userBook.id}`, formData,
       { 
        headers:{
          'Content-Type':'multipart/form-data',
          'Accept':'application/json'
        }, 
        onUploadProgress: onUploadProgress})
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error updating book " + error.response.status + " " + error.response.data.error)
        throw new Error("error updating book " + error.response.status + " " + error)
      }
      console.log("error updating book " + (error as AxiosError).code)
      throw new Error("error updating book " + error)
    }
  }

  findUserBookByCriteria = async (eventType?: ReadingEventType|null, 
    toRead?: boolean|null, page?: number, pageSize?: number) => {
    try {
      const response = await this.apiClient.get<Page<UserBook>>(`${this.API_USERBOOK}`, {
        params: {
          lastEventType: eventType,
          toRead: toRead,
          page: page,
          pageSize: pageSize
        }
      });
      console.log("called userbook by eventtype")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error userbook by eventtype " + (error as AxiosError).code)
      throw new Error("error get userBook by eventType " + error)
    }
  }

  findAuthorByCriteria = async (query?: string|null) => {
    try {
      const response = await this.apiClient.get<Array<Author>>(`${this.API_AUTHOR}`, {
        params: {
          name: query
        }
      });
      console.log("called author by criteria")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error authors by criteria " + (error as AxiosError).code)
      throw new Error("error get authors by criteria " + error)
    }
  }

  findTagsByCriteria = async (query?: string|null) => {
    try {
      const response = await this.apiClient.get<Array<Tag>>(`${this.API_TAG}`, {
        params: {
          name: query
        }
      });
      console.log("called tags by criteria")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error tags by criteria " + (error as AxiosError).code)
      throw new Error("error get tags by criteria " + error)
    }
  }

  getTagById = async (tagId: string) => {
    try {
      const response = await this.apiClient.get<Tag>(`${this.API_TAG}/${tagId}`);
      console.log("called tag by id")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error tag by id " + (error as AxiosError).code)
      throw new Error("error get tag by id " + error)
    }
  }

  getTagBooksById = async (tagId: string, 
    page?: number, pageSize?: number) => {
    try {
      const response = await this.apiClient.get<Page<BookWithUserBook>>(`${this.API_TAG}/${tagId}${this.API_BOOK}`, {
        params: {
          page: page,
          pageSize: pageSize
        }
      });
      console.log("called tag books by id")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error tag books by id " + (error as AxiosError).code)
      throw new Error("error get tag by id " + error)
    }
  }

  logout = async () => {
    try {
      const response = await this.apiClient.post(`${this.API_LOGOUT}`);
      console.log("called logout")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error logout " + (error as AxiosError).code)
      throw new Error("error logout " + error)
    }
  }

  fetchMetadata = async (isbn?: string, title?:string, authors?: string) => {
    try {
      const response = await this.apiClient.get<Metadata>(`${this.API_METADATA}`, {
        params: {
          isbn: isbn,
          title: title,
          authors: authors
        }
      });
      console.log("called metadata")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error metadata " + (error as AxiosError).code)
      throw new Error("error metadata " + error)
    }
  }

  findBooks = async (title?:string, isbn10?: string, isbn13?: string, 
    series?: string, page?: number, pageSize?: number) => {
    try {
      const response = await this.apiClient.get<Page<BookWithUserBook>>(`${this.API_BOOK}`, {
        params: {
          isbn10: isbn10,
          title: title,
          isbn13: isbn13,
          series: series,
          page: page,
          pageSize: pageSize
        }
      });
      console.log("called find books")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error find books " + (error as AxiosError).code)
      throw new Error("error find books " + error)
    }
  }

  deleteUserBook = async (userbookId: string) => {
    try {
      const response = await this.apiClient.delete(`${this.API_USERBOOK}/${userbookId}`);
      console.log("delete userbook")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error delete userbook " + (error as AxiosError).code)
      throw new Error("error delete userbook " + error)
    }
  }

  deleteBook = async (bookId: string) => {
    try {
      const response = await this.apiClient.delete(`${this.API_BOOK}/${bookId}`);
      console.log("delete book")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error delete book " + (error as AxiosError).code)
      throw new Error("error delete book " + error)
    }
  }

  quotes = async (query?:string) => {
    try {
      const response = await this.apiClient.get<Array<Quote>>(`${this.API_QUOTES}`, {
        params: {
          query: query,
        }
      });
      console.log("called quotes")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error quotes " + (error as AxiosError).code)
      throw new Error("error quotes " + error)
    }
  }

  randomQuotes = async () => {
    try {
      const response = await this.apiClient.get<Array<Quote>>(`${this.API_QUOTES}/random`);
      console.log("called random quotes")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error random quotes " + (error as AxiosError).code)
      throw new Error("error random quotes " + error)
    }
  }

  myReadingEvents = async () => {
    try {
      const response = await this.apiClient.get<Page<ReadingEventWithUserBook>>(`${this.API_READING_EVENTS}/me`);
      console.log("called my events")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error my events " + (error as AxiosError).code)
      throw new Error("error my events " + error)
    }
  }

  serverSettings = async () => {
    try {
      const response = await this.apiClient.get<ServerSettings>(`${this.API_SERVER_SETTINGS}`);
      console.log("called server settings")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error server settings " + (error as AxiosError).code)
      throw new Error("error server settings " + error)
    }
  }

  importCsv = async (importConfig: ImportConfigurationDto, file: File, onUploadProgress:any) => {
    try {
      const formData = new FormData()
      formData.append('file', file);
      formData.append('importConfig', new Blob([JSON.stringify(importConfig)], {
        type: "application/json"
    }));
      await this.apiClient.post(this.API_IMPORTS, formData,
       { 
        headers:{
          'Content-Type':'multipart/form-data',
          'Accept':'application/json'
        }, 
        onUploadProgress: onUploadProgress})
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error import csv " + error.response.status + " " + error.response.data.error)
        throw new Error("error import csv " + error.response.status + " " + error)
      }
      console.log("error import csv " + (error as AxiosError).code)
      throw new Error("error importing csv " + error)
    }
  }

}


export default new DataService()
