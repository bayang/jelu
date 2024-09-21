import axios, { AxiosError, AxiosHeaders, AxiosInstance } from "axios";
import { UserBook, Book, UserBookBulkUpdate, UserBookUpdate } from "../model/Book";
import { Author } from "../model/Author";
import router from '../router'
import { CreateUser, UpdateUser, User, UserAuthentication } from "../model/User";
import { CreateReadingEvent, ReadingEvent, ReadingEventType, ReadingEventWithUserBook } from "../model/ReadingEvent";
import { Tag } from "../model/Tag";
import { Metadata } from "../model/Metadata";
import { Page } from "../model/Page";
import { Quote } from "../model/Quote";
import { ServerSettings } from "../model/ServerSettings";
import { ImportConfigurationDto } from "../model/ImportConfiguration";
import qs from "qs";
import dayjs from "dayjs";
import { LibraryFilter } from "../model/LibraryFilter";
import { WikipediaSearchResult } from "../model/WikipediaSearchResult";
import { WikipediaPageResult } from "../model/WikipediaPageResult";
import { MessageCategory, UpdateUserMessage, UserMessage } from "../model/UserMessage";
import { MonthStats, YearStats } from "../model/YearStats";
import { Shelf } from "../model/Shelf";
import { CreateReviewDto, Review, UpdateReviewDto, Visibility } from "../model/Review";
import { Role } from "../model/Role";
import { StringUtils } from "../utils/StringUtils";
import { MetadataRequest } from "../model/MetadataRequest";
import { Series, SeriesUpdate } from "../model/Series";
import { DirectoryListing } from "../model/DirectoryListing";
import { BookQuote, CreateBookQuoteDto, UpdateBookQuoteDto } from "../model/BookQuote";

class DataService {

  private apiClient: AxiosInstance;

  private token?: string = '';

  private TOKEN_KEY = 'jelu-token'

  private API_BOOK = '/books';

  private API_USERBOOK = '/userbooks';

  private API_USER = '/users';

  private API_AUTHOR = '/authors';

  private API_TAG = '/tags';

  private API_SERIES = '/series';

  private API_LOGOUT = '/logout';

  private API_METADATA = '/metadata';

  private API_QUOTES = '/quotes';

  private API_READING_EVENTS = '/reading-events';

  private API_SERVER_SETTINGS = '/server-settings';

  private API_IMPORTS = '/imports';

  private API_EXPORTS = '/exports';

  private API_WIKIPEDIA = '/wikipedia';

  private API_SEARCH = '/search';

  private API_PAGE = '/page';

  private API_MERGE = '/merge';

  private API_USER_MESSAGES = '/user-messages';

  private API_STATS = '/stats';

  private API_SHELVES = '/shelves';

  private API_REVIEWS = '/reviews';
  
  private API_BOOK_QUOTES = '/book-quotes';

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
          config.headers = new AxiosHeaders()
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
        if (error != null && error.response != null && error.response.status === 401) {
          router.push({ name: 'login' }).then(() => { console.log("ok nav in interceptor") }).catch(() => { console.log("error nav in interceptor") })
        } else {
          throw error
        }
      });
  }

  getToken = (): string | null => {
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

  getUserBookById = async (userBookId: string) => {
    try {
      const response = await this.apiClient.get<UserBook>(`${this.API_USERBOOK}/${userBookId}`, {
        transformResponse: this.transformUserbook
      });
      console.log("called userBook " + userBookId)
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error userbook by id " + (error as AxiosError).code)
      throw new Error("error finding userBook " + userBookId + " " + error)
    }
  }

  /*
  * Dates are deserialized as strings, convert to Date instead
  */
  transformUserbook = (data: string) => {
    const tr = JSON.parse(data)
    if (tr.readingEvents != null && tr.readingEvents.length > 0) {
      for (const ev of tr.readingEvents) {
        if (ev.modificationDate != null) {
          ev.modificationDate = dayjs(ev.modificationDate).toDate()
        }
        if (ev.startDate != null) {
          ev.startDate = dayjs(ev.startDate).toDate()
        }
        if (ev.endDate != null) {
          ev.endDate = dayjs(ev.endDate).toDate()
        }
      }
    }
    return tr
  }

  getUser = async () => {
    try {
      const response = await this.apiClient.get<UserAuthentication>(`${this.API_USER}/me`)
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

  getUsers = async () => {
    try {
      const response = await this.apiClient.get<Array<User>>(this.API_USER)
      console.log("called users")
      console.log(response.data)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios users " + error.response.status + " " + error.response.data.error)
      }
      console.log("error users " + (error as AxiosError).code)
      throw new Error("error users " + error)
    }
  }

  getUserById = async (userId: string) => {
    try {
      const response = await this.apiClient.get<User>(`${this.API_USER}/${userId}`);
      console.log("called user by id")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error user by id " + (error as AxiosError).code)
      throw new Error("error get user by id " + error)
    }
  }

  authenticateUser = async (login: string, password: string) => {
    try {
      const response = await this.apiClient.get<UserAuthentication>(`${this.API_USER}/me`, {
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
        localStorage.setItem(this.TOKEN_KEY, this.token)
      }
      return response.data.user

    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios auth user " + error.response.status + " " + error.response.data.error)
        throw new Error("login error " + error.response.status + " " + error)
      }
      console.log("error auth user " + (error as AxiosError))
      throw new Error("login error, backend seems down or unreachable")
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

  deleteUser = async (userId: string) => {
    try {
      const response = await this.apiClient.delete(`${this.API_USER}/${userId}`);
      console.log("called delete user")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error delete user " + (error as AxiosError).code)
      throw new Error("error delete user " + error)
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

  createUser = async (user: CreateUser) => {
    try {
      const resp = await this.apiClient.post<User>(`${this.API_USER}`, user)
      console.log('create user ')
      console.log(resp)
      console.log(resp.data)
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error create user " + error.response.status + " " + error.response.data)
        console.log(error.response.data)
        throw new Error("Error ! " + error.response.data.message)
      }
      console.log("error create user " + (error as AxiosError).code)
      throw new Error("error create user " + error)
    }
  }

  updateUser = async (userId: string, user: UpdateUser) => {
    try {
      const resp = await this.apiClient.put<User>(`${this.API_USER}/${userId}`, user)
      console.log('update user ')
      console.log(resp)
      console.log(resp.data)
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error update user " + error.response.status + " " + error.response.data)
        console.log(error.response.data)
        throw new Error("Error ! " + error.response.data.message)
      }
      console.log("error update user " + (error as AxiosError).code)
      throw new Error("error update user " + error)
    }
  }

  createInitialUser = async (login: string, password: string) => {
    try {
      const resp = await this.apiClient.post<User>(`${this.API_USER}`, {
        'login': login,
        'password': password,
        'isAdmin': true
      },
        {
          auth: {
            username: 'setup',
            password: 'initial',
          },
        })
      console.log('create initial user')
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

  saveBookImage = async (book: Book, file: File | null, onUploadProgress: any) => {
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
          headers: {
            'Content-Type': 'multipart/form-data',
            'Accept': 'application/json'
          },
          onUploadProgress: onUploadProgress
        })
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

  saveUserBookImage = async (userBook: UserBook, file: File | null, onUploadProgress: any) => {
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
          headers: {
            'Content-Type': 'multipart/form-data',
            'Accept': 'application/json'
          },
          onUploadProgress: onUploadProgress
        })
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

  updateUserBookImage = async (userBook: UserBook, file: File | null, onUploadProgress: any) => {
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
          headers: {
            'Content-Type': 'multipart/form-data',
            'Accept': 'application/json'
          },
          onUploadProgress: onUploadProgress
        })
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

  updateUserBook = async (userBook: UserBookUpdate) => {
    try {
      const resp = await this.apiClient.put<UserBook>(`${this.API_USERBOOK}/${userBook.id}`, userBook)
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

  findUserBookByCriteria = async (lastEventTypes?: Array<ReadingEventType> | null, bookId?: string|null,
    userId?: string|null, toRead?: boolean | null, owned?: boolean | null, borrowed?: boolean | null, 
    page?: number, size?: number, sort?: string) => {
    try {
      const response = await this.apiClient.get<Page<UserBook>>(`${this.API_USERBOOK}`, {
        params: {
          lastEventTypes: lastEventTypes,
          bookId: bookId,
          userId: userId,
          toRead: toRead,
          owned: owned,
          borrowed: borrowed,
          page: page,
          size: size,
          sort: sort
        },
        paramsSerializer: {
          serialize : (params) => {
            return qs.stringify(params, { arrayFormat: 'comma' })
        }},
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

  findAuthorByCriteria = async (query?: string | null) => {
    try {
      const response = await this.apiClient.get<Page<Author>>(`${this.API_AUTHOR}`, {
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

  findTagsByCriteria = async (query?: string | null) => {
    try {
      const response = await this.apiClient.get<Page<Tag>>(`${this.API_TAG}`, {
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

  findSeriesByCriteria = async (query?: string | null) => {
    try {
      const response = await this.apiClient.get<Page<Series>>(`${this.API_SERIES}`, {
        params: {
          name: query
        }
      });
      console.log("called series by criteria")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error series by criteria " + (error as AxiosError).code)
      throw new Error("error get series by criteria " + error)
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

  getSeriesById = async (seriesId: string) => {
    try {
      const response = await this.apiClient.get<Series>(`${this.API_SERIES}/${seriesId}`);
      console.log("called series by id")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error series by id " + (error as AxiosError).code)
      throw new Error("error get series by id " + error)
    }
  }

  getAuthorById = async (authorId: string) => {
    try {
      const response = await this.apiClient.get<Author>(`${this.API_AUTHOR}/${authorId}`, {
        transformResponse: this.transformAuthor
      });
      console.log("called author by id")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error author by id " + (error as AxiosError).code)
      throw new Error("error get author by id " + error)
    }
  }

  /*
  * Dates are deserialized as strings, convert to Date instead
  */
  transformAuthor = (data: string) => {
    const tr = JSON.parse(data)
    if (tr.dateOfBirth != null) {
      tr.dateOfBirth = dayjs(tr.dateOfBirth).toDate()
    }
    if (tr.dateOfDeath != null) {
      tr.dateOfDeath = dayjs(tr.dateOfDeath).toDate()
    }
    return tr
  }

  getTagBooksById = async (tagId: string,
    page?: number, size?: number, sort?: string, libraryFilter?: LibraryFilter, lastEventTypes?: Array<ReadingEventType> | null) => {
    try {
      const response = await this.apiClient.get<Page<Book>>(`${this.API_TAG}/${tagId}${this.API_BOOK}`, {
        params: {
          page: page,
          size: size,
          sort: sort,
          libraryFilter: libraryFilter,
          lastEventTypes: lastEventTypes,
        },
        paramsSerializer: {
          serialize : (params) => {
            return qs.stringify(params, { arrayFormat: 'comma' })
        }},
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
      throw new Error("error get tag books by id " + error)
    }
  }

  getOrphanTags = async (page?: number, size?: number, sort?: string) => {
    try {
      const response = await this.apiClient.get<Page<Tag>>(`${this.API_TAG}/orphans`, {
        params: {
          page: page,
          size: size,
          sort: sort,
        }
      });
      console.log("called tag orphans")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error tag orphans " + (error as AxiosError).code)
      throw new Error("error get tag orphans " + error)
    }
  }

  getSeriesBooksById = async (seriesId: string,
    page?: number, size?: number, sort?: string, libraryFilter?: LibraryFilter) => {
    try {
      const response = await this.apiClient.get<Page<Book>>(`${this.API_SERIES}/${seriesId}${this.API_BOOK}`, {
        params: {
          page: page,
          size: size,
          sort: sort,
          libraryFilter: libraryFilter
        }
      });
      console.log("called series books by id")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error series books by id " + (error as AxiosError).code)
      throw new Error("error get series books by id " + error)
    }
  }

  getAuthorBooksById = async (authorId: string,
    page?: number, size?: number, sort?: string, libraryFilter?: LibraryFilter,
    roleFilter?: Role) => {
    try {
      const response = await this.apiClient.get<Page<Book>>(`${this.API_AUTHOR}/${authorId}${this.API_BOOK}`, {
        params: {
          page: page,
          size: size,
          sort: sort,
          libraryFilter: libraryFilter,
          roleFilter: roleFilter
        }
      });
      console.log("called author books by id")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error author books by id " + (error as AxiosError).code)
      throw new Error("error get author books by id " + error)
    }
  }

  logout = async () => {
    try {
      const response = await this.apiClient.post(`${this.API_LOGOUT}`);
      console.log("called logout")
      console.log(response)
      localStorage.removeItem(this.TOKEN_KEY)
      this.token = ''
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

  fetchMetadata = async (isbn?: string, title?: string, authors?: string) => {
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

  fetchMetadataWithPlugins = async (metadataRequest: MetadataRequest) => {
    try {
      
      const response = await this.apiClient.post<Metadata>(`${this.API_METADATA}`, metadataRequest)
      console.log("called metadata with plugins")
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

  findBooksDetailed = async (title?: string, isbn10?: string, isbn13?: string,
    series?: string, authors?: Array<string>, translators?: Array<string>, 
    tags?: Array<string>, page?: number, size?: number, sort?: string,
    libraryFilter?: LibraryFilter) => {
    try {
      const response = await this.apiClient.get<Page<Book>>(`${this.API_BOOK}`, {
        params: {
          isbn10: isbn10,
          title: title,
          isbn13: isbn13,
          series: series,
          authors: authors,
          translators: translators,
          tags: tags,
          page: page,
          size: size,
          sort: sort,
          libraryFilter: libraryFilter
        },
        paramsSerializer: {
          serialize : (params) => {
            return qs.stringify(params, { arrayFormat: 'comma' })
        }},
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

  findBooks = async (query?: string, page?: number, size?: number, sort?: string,
    libraryFilter?: LibraryFilter) => {
    try {
      const response = await this.apiClient.get<Page<Book>>(`${this.API_BOOK}`, {
        params: {
          q: query,
          page: page,
          size: size,
          sort: sort,
          libraryFilter: libraryFilter
        },
        paramsSerializer: {
          serialize : (params) => {
            return qs.stringify(params, { arrayFormat: 'comma' })
        }},
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

  deleteReadingEvent = async (eventId: string) => {
    try {
      const response = await this.apiClient.delete(`${this.API_READING_EVENTS}/${eventId}`);
      console.log("delete event")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error delete event " + (error as AxiosError).code)
      throw new Error("error delete event " + error)
    }
  }

  deleteTag = async (tagId: string) => {
    try {
      const response = await this.apiClient.delete(`${this.API_TAG}/${tagId}`);
      console.log("delete tag")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error delete tag " + (error as AxiosError).code)
      throw new Error("error delete tag " + error)
    }
  }

  quotes = async (query?: string) => {
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

  /*
  * Dates are deserialized as strings, convert to Date instead
  */
  transformReadingEvents = (data: string) => {
    const page = JSON.parse(data)
    if (page.content) {
      for (const ev of page.content) {
        if (ev.modificationDate != null) {
          ev.modificationDate = dayjs(ev.modificationDate).toDate()
        }
        if (ev.startDate != null) {
          ev.startDate = dayjs(ev.startDate).toDate()
        }
        if (ev.endDate != null) {
          ev.endDate = dayjs(ev.endDate).toDate()
        }
      }
    }
    return page
  }

  myReadingEvents = async (eventTypes?: Array<ReadingEventType> | null, bookId?: string,
    startedAfter?: string, startedBefore?: string,
    endedAfter?: string, endedBefore?: string,
    page?: number, size?: number, sort?: string) => {
    try {
      const response = await this.apiClient.get<Page<ReadingEventWithUserBook>>(`${this.API_READING_EVENTS}/me`, {
        params: {
          eventTypes: eventTypes,
          bookId: bookId,
          startedAfter: startedAfter,
          startedBefore: startedBefore,
          endedAfter: endedAfter,
          endedBefore: endedBefore,
          page: page,
          size: size,
          sort: sort
        },
        paramsSerializer: {
          serialize : (params) => {
            return qs.stringify(params, { arrayFormat: 'comma' })
        }},
        transformResponse: this.transformReadingEvents
      });
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

  findReadingEvents = async (eventTypes?: Array<ReadingEventType> | null, userId?: string, bookId?: string,
    startedAfter?: string, startedBefore?: string,
    endedAfter?: string, endedBefore?: string,
    page?: number, size?: number, sort?: string) => {
    try {
      const response = await this.apiClient.get<Page<ReadingEventWithUserBook>>(`${this.API_READING_EVENTS}`, {
        params: {
          eventTypes: eventTypes,
          userId: userId,
          bookId: bookId,
          startedAfter: startedAfter,
          startedBefore: startedBefore,
          endedAfter: endedAfter,
          endedBefore: endedBefore,
          page: page,
          size: size,
          sort: sort
        },
        paramsSerializer: {
          serialize : (params) => {
            return qs.stringify(params, { arrayFormat: 'comma' })
        }},
        transformResponse: this.transformReadingEvents
      });
      console.log("called events")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error events " + (error as AxiosError).code)
      throw new Error("error events " + error)
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

  importCsv = async (importConfig: ImportConfigurationDto, file: File, onUploadProgress: any) => {
    try {
      const formData = new FormData()
      formData.append('file', file);
      formData.append('importConfig', new Blob([JSON.stringify(importConfig)], {
        type: "application/json"
      }));
      await this.apiClient.post(this.API_IMPORTS, formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'Accept': 'application/json'
          },
          onUploadProgress: onUploadProgress
        })
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error import csv " + error.response.status + " " + error.response.data.error)
        throw new Error("error import csv " + error.response.status + " " + error)
      }
      console.log("error import csv " + (error as AxiosError).code)
      throw new Error("error importing csv " + error)
    }
  }

  exportCsv = async () => {
    try {
      await this.apiClient.post(this.API_EXPORTS)
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error export csv " + error.response.status + " " + error.response.data.error)
        throw new Error("error export csv " + error.response.status + " " + error)
      }
      console.log("error export csv " + (error as AxiosError).code)
      throw new Error("error exporting csv request" + error)
    }
  }

  updateReadingEvent = async (event: ReadingEvent) => {
    try {
      const resp = await this.apiClient.put<ReadingEvent>(`${this.API_READING_EVENTS}/${event.id}`, {
        eventType: event.eventType,
        eventDate: event.endDate,
        startDate: event.startDate
      })
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error updating event " + error.response.status + " " + error.response.data.error)
        throw new Error("error updating event " + error.response.status + " " + error)
      }
      console.log("error updating event " + (error as AxiosError).code)
      throw new Error("error updating event " + error)
    }
  }

  createReadingEvent = async (event: CreateReadingEvent) => {
    try {
      const resp = await this.apiClient.post<ReadingEvent>(`${this.API_READING_EVENTS}`, event)
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error creating event " + error.response.status + " " + error.response.data.error)
        throw new Error("error creating event " + error.response.status + " " + error)
      }
      console.log("error creating event " + (error as AxiosError).code)
      throw new Error("error creating event " + error)
    }
  }

  wikipediaSearch = async (query: string, language: string) => {
    try {
      const response = await this.apiClient.get<WikipediaSearchResult>(`${this.API_WIKIPEDIA}${this.API_SEARCH}`, {
        params: {
          query: query,
          language: language
        }
      });
      console.log("called wikipedia search")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error wikipedia search " + (error as AxiosError).code)
      throw new Error("error wikipedia search " + error)
    }
  }

  wikipediaPage = async (pageTitle: string, language: string) => {
    try {
      const response = await this.apiClient.get<WikipediaPageResult>(`${this.API_WIKIPEDIA}${this.API_PAGE}`, {
        params: {
          pageTitle: pageTitle,
          language: language
        }
      });
      console.log("called wikipedia page")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error wikipedia page " + (error as AxiosError).code)
      throw new Error("error wikipedia page " + error)
    }
  }

  updateAuthor = async (author: Author, file: File | null, onUploadProgress: any) => {
    try {
      const formData = new FormData()
      if (file != null) {
        formData.append('file', file);
      }
      formData.append('author', new Blob([JSON.stringify(author)], {
        type: "application/json"
      }));
      const resp = await this.apiClient.put<Author>(`${this.API_AUTHOR}/${author.id}`, formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'Accept': 'application/json'
          },
          onUploadProgress: onUploadProgress
        })
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

  mergeAuthors = async (authorId: string, otherId: string, authorDto: Author) => {
    try {
      const resp = await this.apiClient.put<Author>(`${this.API_AUTHOR}/${authorId}${this.API_MERGE}/${otherId}`, authorDto)
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error merging authors " + error.response.status + " " + error.response.data.error)
        throw new Error("error merging authors " + error.response.status + " " + error)
      }
      console.log("error merging authors " + (error as AxiosError).code)
      throw new Error("error merging authors " + error)
    }
  }

  /*
  * Dates are deserialized as strings, convert to Date instead
  */
  transformUserMessage = (data: string) => {
    const ev = JSON.parse(data)
    if (ev.modificationDate != null) {
      ev.modificationDate = dayjs(ev.modificationDate).toDate()
    }
    return ev
  }

  messages = async (messageCategories?: Array<MessageCategory> | null, read?: boolean,
    page?: number, size?: number, sort?: string) => {
    try {
      const response = await this.apiClient.get<Page<UserMessage>>(`${this.API_USER_MESSAGES}`, {
        params: {
          messageCategories: messageCategories,
          read: read,
          page: page,
          size: size,
          sort: sort
        },
        paramsSerializer: {
          serialize : (params) => {
            return qs.stringify(params, { arrayFormat: 'comma' })
        }},
        transformResponse: this.transformUserMessage
      });
      console.log("called userMessages")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error userMessages " + (error as AxiosError).code)
      throw new Error("error userMessages " + error)
    }
  }

  updateUserMessage = async (messageId: string, updateDto: UpdateUserMessage) => {
    try {
      const response = await this.apiClient.put<UserMessage>(`${this.API_USER_MESSAGES}/${messageId}`, updateDto);
      console.log("called update userMessage")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error update userMessage " + (error as AxiosError).code)
      throw new Error("error update userMessage " + error)
    }
  }

  yearStats = async () => {
    try {
      const response = await this.apiClient.get<Array<YearStats>>(`${this.API_STATS}`);
      console.log("called stats")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error stats " + (error as AxiosError).code)
      throw new Error("error stats " + error)
    }
  }

  monthStatsForYear = async (year: number) => {
    try {
      const response = await this.apiClient.get<Array<MonthStats>>(`${this.API_STATS}/${year}`);
      console.log("called stats months")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error stats months " + (error as AxiosError).code)
      throw new Error("error stats months " + error)
    }
  }

  yearsWithStats = async () => {
    try {
      const response = await this.apiClient.get<Array<number>>(`${this.API_STATS}/years`);
      console.log("called stats years")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error stats years " + (error as AxiosError).code)
      throw new Error("error stats years " + error)
    }
  }

  shelves = async (name?: string, targetId?: string) => {
    try {
      const response = await this.apiClient.get<Array<Shelf>>(`${this.API_SHELVES}`, {
        params: {
          name: name,
          targetId: targetId
        }
      });
      console.log("called shelves")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error shelves " + (error as AxiosError).code)
      throw new Error("error shelves " + error)
    }
  }

  deleteShelf = async (shelfId?: string) => {
    try {
      const response = await this.apiClient.delete(`${this.API_SHELVES}/${shelfId}`);
      console.log("called delete shelves")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error delete shelves " + (error as AxiosError).code)
      throw new Error("error delete shelves " + error)
    }
  }

  saveShelf = async (shelf: Shelf) => {
    try {
      const resp = await this.apiClient.post<Shelf>(`${this.API_SHELVES}`, shelf)
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error creating shelf " + error.response.status + " " + error.response.data.error)
        throw new Error("error creating shelf " + error.response.status + " " + error)
      }
      console.log("error creating shelf " + (error as AxiosError).code)
      throw new Error("error creating event " + error)
    }
  }

  bulkEditUserBooks = async (bulkUpdateDto: UserBookBulkUpdate) => {
    try {
      const resp = await this.apiClient.put<number>(this.API_USERBOOK, {
        ids: bulkUpdateDto.ids,
        addTags: bulkUpdateDto.addTags,
        removeTags: bulkUpdateDto.removeTags,
        owned: bulkUpdateDto.owned,
        toRead: bulkUpdateDto.toRead,
      })
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error bulk updating " + error.response.status + " " + error.response.data.error)
        throw new Error("error bulk updating " + error.response.status + " " + error)
      }
      console.log("error bulk updating " + (error as AxiosError).code)
      throw new Error("error bulk updating " + error)
    }
  }

  saveReview = async (review: CreateReviewDto) => {
    try {
      const resp = await this.apiClient.post<Review>(`${this.API_REVIEWS}`, review)
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error creating review " + error.response.status + " " + error.response.data.error)
        throw new Error("error creating review " + error.response.status + " " + error)
      }
      console.log("error creating review " + (error as AxiosError).code)
      throw new Error("error creating review " + error)
    }
  }

  /*
  * Dates are deserialized as strings, convert to Date instead
  */
  transformReviews = (data: string) => {
    const page = JSON.parse(data)
    if (page.content) {
      for (const ev of page.content) {
        if (ev.modificationDate != null) {
          ev.modificationDate = dayjs(ev.modificationDate).toDate()
        }
        if (ev.creationDate != null) {
          ev.creationDate = dayjs(ev.creationDate).toDate()
        }
        if (ev.reviewDate != null) {
          ev.reviewDate = dayjs(ev.reviewDate).toDate()
        }
      }
    }
    return page
  }

  transformReview = (data: string) => {
    const ev = JSON.parse(data)
    if (ev.modificationDate != null) {
      ev.modificationDate = dayjs(ev.modificationDate).toDate()
    }
    if (ev.creationDate != null) {
      ev.creationDate = dayjs(ev.creationDate).toDate()
    }
    if (ev.reviewDate != null) {
      ev.reviewDate = dayjs(ev.reviewDate).toDate()
    }
    return ev
  }

  findReviews = async (userId?: string, bookId?: string, visibility: Visibility | null = null,
    after: string | null = null, before: string | null = null,
    page?: number, size?: number, sort: string | null = null) => {
    try {
      const response = await this.apiClient.get<Page<Review>>(`${this.API_REVIEWS}`, {
        params: {
          userId: userId,
          bookId: bookId,
          visibility: visibility,
          after: after,
          before: before,
          page: page,
          size: size,
          sort: sort
        },
        transformResponse: this.transformReviews
      });
      console.log("called reviews")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error reviews " + (error as AxiosError).code)
      throw new Error("error reviews " + error)
    }
  }

  findReviewById = async (reviewId: string) => {
    try {
      const response = await this.apiClient.get<Review>(`${this.API_REVIEWS}/${reviewId}`, {
        transformResponse: this.transformReview
      });
      console.log("called review")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error review " + (error as AxiosError).code)
      throw new Error("error review " + error)
    }
  }

  deleteReview = async (reviewId: string) => {
    try {
      const response = await this.apiClient.delete(`${this.API_REVIEWS}/${reviewId}`);
      console.log("delete review")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error delete review " + (error as AxiosError).code)
      throw new Error("error delete review " + error)
    }
  }

  updateReview = async (reviewId: string, updateDto: UpdateReviewDto) => {
    try {
      const response = await this.apiClient.put<Review>(`${this.API_REVIEWS}/${reviewId}`, updateDto);
      console.log("called update review")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error update review " + (error as AxiosError).code)
      throw new Error("error update review " + error)
    }
  }
  
  updateSeries = async (seriesId: string, updateDto: SeriesUpdate) => {
    try {
      const response = await this.apiClient.put<Series>(`${this.API_SERIES}/${seriesId}`, updateDto);
      console.log("called update series")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error update series " + (error as AxiosError).code)
      throw new Error("error update series " + error)
    }
  }

  updateBook = async (bookId: string, bookUpdateDto: Book) => {
    try {
      const response = await this.apiClient.put<Book>(`${this.API_BOOK}/${bookId}`, bookUpdateDto);
      console.log("called update book")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error update book " + (error as AxiosError).code)
      throw new Error("error update book " + error)
    }
  }

  findBookById = async (bookId: string) => {
    try {
      const response = await this.apiClient.get<Book>(`${this.API_BOOK}/${bookId}`);
      console.log("called book by id")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error review " + (error as AxiosError).code)
      throw new Error("error book by id " + error)
    }
  }

  usernameById = async (userId: string) => {
    try {
      const response = await this.apiClient.get(`${this.API_USER}/${userId}/name`);
      console.log("called username by id")
      console.log(response)
      return response.data.username;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error review " + (error as AxiosError).code)
      throw new Error("error username by id " + error)
    }
  }

  checkIsbnExists = async (isbn10: string|undefined, isbn13: string|undefined) => {
    console.log(isbn10 + " " + isbn13)
    if (StringUtils.isNotBlank(isbn10)) {
      const res = await this.findBooks(`isbn:${isbn10}`)
      console.log(res.empty)
      if (!res.empty) {
        return res.content[0]
      }
    }
    if (StringUtils.isNotBlank(isbn13)) {
      console.log(isbn13)
      const res = await this.findBooks(`isbn:${isbn13}`)
      console.log(res.empty)
      if (!res.empty) {
        return res.content[0]
      }
    }
    return null
  }

  getDirectoryListing = async (path: string, reason = "metadata") => {
    try {
      const response = await this.apiClient.post<DirectoryListing>('/filesystem', {'reason' : reason, 'path' : path});
      console.log("called directory " + path)
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error directory " + (error as AxiosError).code)
      throw new Error("error directory " + path + " " + error)
    }
  }
  
  getMetadataFromUploadedFile = async (file: File | null, onUploadProgress: any) => {
    try {
      const formData = new FormData()
      if (file != null) {
        formData.append('file', file);
      }
      const resp = await this.apiClient.post<Metadata>(`${this.API_METADATA}/file`, formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'Accept': 'application/json'
          },
          onUploadProgress: onUploadProgress
        })
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error uploading file " + error.response.status + " " + error.response.data.error)
        throw new Error("error uploading file " + error.response.status + " " + error)
      }
      console.log("error uploading file " + (error as AxiosError).code)
      throw new Error("error uploading file " + error)
    }
  }

  getMetadataFromFile = async (filePath: string) => {
    try {
      const response = await this.apiClient.get<Metadata>(`${this.API_METADATA}/file`, {
        params: {
          filepath: filePath,
        }
      });
      console.log("called get metadata from file")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error metadat from path " + (error as AxiosError).code)
      throw new Error("error metadata from path " + error)
    }
  }
  
  saveBookQuote = async (quote: CreateBookQuoteDto) => {
    try {
      const resp = await this.apiClient.post<BookQuote>(`${this.API_BOOK_QUOTES}`, quote)
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error creating book quote " + error.response.status + " " + error.response.data.error)
        throw new Error("error creating book quote " + error.response.status + " " + error)
      }
      console.log("error creating book quote " + (error as AxiosError).code)
      throw new Error("error creating book quote " + error)
    }
  }

  /*
  * Dates are deserialized as strings, convert to Date instead
  */
  transformBookQuotes = (data: string) => {
    const page = JSON.parse(data)
    if (page.content) {
      for (const ev of page.content) {
        if (ev.modificationDate != null) {
          ev.modificationDate = dayjs(ev.modificationDate).toDate()
        }
        if (ev.creationDate != null) {
          ev.creationDate = dayjs(ev.creationDate).toDate()
        }
      }
    }
    return page
  }

  transformBookQuote = (data: string) => {
    const ev = JSON.parse(data)
    if (ev.modificationDate != null) {
      ev.modificationDate = dayjs(ev.modificationDate).toDate()
    }
    if (ev.creationDate != null) {
      ev.creationDate = dayjs(ev.creationDate).toDate()
    }
    return ev
  }

  findBookQuotes = async (userId?: string, bookId?: string, visibility: Visibility | null = null,
    page?: number, size?: number, sort: string | null = null) => {
    try {
      const response = await this.apiClient.get<Page<BookQuote>>(`${this.API_BOOK_QUOTES}`, {
        params: {
          userId: userId,
          bookId: bookId,
          visibility: visibility,
          page: page,
          size: size,
          sort: sort
        },
        transformResponse: this.transformBookQuotes
      });
      console.log("called book quotes")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error book quotes " + (error as AxiosError).code)
      throw new Error("error book quotes " + error)
    }
  }
  
  findBookQuoteById = async (quoteId: string) => {
    try {
      const response = await this.apiClient.get<BookQuote>(`${this.API_BOOK_QUOTES}/${quoteId}`, {
        transformResponse: this.transformBookQuote
      });
      console.log("called book quote")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error book quote " + (error as AxiosError).code)
      throw new Error("error book quote " + error)
    }
  }

  deleteBookQuote = async (quoteId: string) => {
    try {
      const response = await this.apiClient.delete(`${this.API_BOOK_QUOTES}/${quoteId}`);
      console.log("delete quote")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error delete quote " + (error as AxiosError).code)
      throw new Error("error delete quote " + error)
    }
  }

  updateBookQuote = async (quoteId: string, updateDto: UpdateBookQuoteDto) => {
    try {
      const response = await this.apiClient.put<BookQuote>(`${this.API_BOOK_QUOTES}/${quoteId}`, updateDto);
      console.log("called update quote")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error update quote " + (error as AxiosError).code)
      throw new Error("error update quote " + error)
    }
  }

}

export default new DataService()
