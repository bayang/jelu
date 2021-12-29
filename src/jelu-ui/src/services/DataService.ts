import axios, { AxiosError, AxiosInstance } from "axios";
import { UserBook, Book } from "../model/Book";
import { Author } from "../model/Author";
import router from '../router'
import { User, UserAuthentication } from "../model/User";
import { JeluError } from "../model/JeluError";
import { ReadingEventType } from "../model/ReadingEvent";
import { Tag, TagWithBooks } from "../model/Tag";
import { Metadata } from "../model/Metadata";


class DataService {

  private apiClient: AxiosInstance;
  
  private token?: string = ''

  private TOKEN_KEY: string = 'jelu-token'

  private API_BOOK = '/books';

  private API_USERBOOK = '/userbooks';

  private API_AUTHOR = '/authors';

  private API_TAG = '/tags';

  private API_LOGOUT = '/logout';

  private API_METADATA = '/metadata';
  
  constructor() {
    this.apiClient = axios.create({
      baseURL: "http://localhost:11111/api",
      headers: {
        "Content-type": "application/json",
        'X-Requested-With': 'XMLHttpRequest'
      },
      withCredentials: true,
    });
    
    this.apiClient.interceptors.request.use((config) => {
      let tok = this.getToken()
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
      router.push({name: 'login'}).then(() => {console.log("ok nav in interceptor")}).catch(() => {console.log("error nav in interceptor")})
    });
  }
  
  getToken = ():string | null => {
    if (this.token != null && this.token.trim().length > 0) {
      console.log('get tok from property')
      localStorage.setItem(this.TOKEN_KEY, this.token)
      return this.token!
    }
    else if (localStorage.getItem(this.TOKEN_KEY) != null) {
      console.log('get tok from storage')
      return localStorage.getItem(this.TOKEN_KEY)
    }
    else {
      return null
    }
  }

  findUserBooks = async () => {
    try {
      const response = await this.apiClient.get<Array<UserBook>>(`${this.API_USERBOOK}/me`);
      console.log("called backend")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error findall " + (error as AxiosError).toJSON())
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
      console.log("error findall " + (error as AxiosError).toJSON())
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
      console.log("error user " + (error as AxiosError).toJSON())
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
        localStorage.setItem("jelu-token", this.token!)
      }
      return response.data.user

    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios auth user " + error.response.status + " " + error.response.data.error)
        throw new Error("login error " + error.response.status + " " + error)
      }
      console.log("error auth user " + (error as AxiosError).toJSON())
      console.log("error auth user " + (error as AxiosError).code)
      throw new Error("login error " + error)
    }
  }

  fetchToken = async (login?: string, password?: string) => {
    let config = {}
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
      console.log("error auth token " + (error as AxiosError).toJSON())
      console.log("error auth token " + (error as AxiosError).code)
      throw new Error("error auth token " + error)
    }
  }

  setupStatus = async () => {
    try {
      let response = await this.apiClient.get('/setup/status')
      console.log(`setup resp `)
      console.log(response.data)
      return response.data.isInitialSetup
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios setup " + error.response.status + " " + error.response.data.error)
      }
      console.log("error setup " + (error as AxiosError).toJSON())
      console.log("error setup " + (error as AxiosError).code)
      throw new Error("error setup " + error)
    }
  }

  createUser = async (login: string, password: string) => {
    try {
      let resp = await this.apiClient.post<User>('/users', {
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
      console.log("error create user " + (error as AxiosError).toJSON())
      console.log("error create user " + (error as AxiosError).code)
      throw new Error("error create user " + error)
    }
  }

  saveBook = async (book: Book) => {
    try {
      let resp = await this.apiClient.post<Book>(this.API_BOOK, book)
      return resp.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error saving book " + error.response.status + " " + error.response.data.error)
        throw new Error("error saving book " + error.response.status + " " + error)
      }
      console.log("error saving book " + (error as AxiosError).toJSON())
      console.log("error saving book " + (error as AxiosError).code)
      throw new Error("error saving book " + error)
    }
  }

  saveBookImage = async (book: Book, file: File|null, onUploadProgress:any) => {
    try {
      let formData = new FormData()
      if (file != null) {
        formData.append('file', file);
      }
      formData.append('book', new Blob([JSON.stringify(book)], {
        type: "application/json"
    }));
      let resp = await this.apiClient.post<Book>(this.API_BOOK, formData,
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
      console.log("error saving book " + (error as AxiosError).toJSON())
      console.log("error saving book " + (error as AxiosError).code)
      throw new Error("error saving book " + error)
    }
  }

  saveUserBookImage = async (userBook: UserBook, file: File|null, onUploadProgress:any) => {
    try {
      let formData = new FormData()
      if (file != null) {
        formData.append('file', file);
      }
      formData.append('book', new Blob([JSON.stringify(userBook)], {
        type: "application/json"
    }));
      let resp = await this.apiClient.post<UserBook>(this.API_USERBOOK, formData,
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
      console.log("error saving book " + (error as AxiosError).toJSON())
      console.log("error saving book " + (error as AxiosError).code)
      throw new Error("error saving book " + error)
    }
  }

  updateUserBookImage = async (userBook: UserBook, file: File|null, onUploadProgress:any) => {
    try {
      let formData = new FormData()
      if (file != null) {
        formData.append('file', file);
      }
      formData.append('book', new Blob([JSON.stringify(userBook)], {
        type: "application/json"
    }));
      let resp = await this.apiClient.put<UserBook>(`${this.API_USERBOOK}/${userBook.id}`, formData,
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
      console.log("error updating book " + (error as AxiosError).toJSON())
      console.log("error updating book " + (error as AxiosError).code)
      throw new Error("error updating book " + error)
    }
  }

  findUserBookByCriteria = async (eventType?: ReadingEventType|null, toRead?: boolean|null) => {
    try {
      const response = await this.apiClient.get<Array<UserBook>>(`${this.API_USERBOOK}`, {
        params: {
          lastEventType: eventType,
          toRead: toRead
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
      console.log("error userbook by eventtype " + (error as AxiosError).toJSON())
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
      console.log("error authors by criteria " + (error as AxiosError).toJSON())
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
      console.log("error tags by criteria " + (error as AxiosError).toJSON())
      console.log("error tags by criteria " + (error as AxiosError).code)
      throw new Error("error get tags by criteria " + error)
    }
  }

  getTagById = async (tagId: string) => {
    try {
      const response = await this.apiClient.get<TagWithBooks>(`${this.API_TAG}/${tagId}`);
      console.log("called tags by id")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
      }
      console.log("error tags by id " + (error as AxiosError).toJSON())
      console.log("error tags by id " + (error as AxiosError).code)
      throw new Error("error get tags by id " + error)
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
      console.log("error logout " + (error as AxiosError).toJSON())
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
      console.log("error metadata " + (error as AxiosError).toJSON())
      console.log("error metadata " + (error as AxiosError).code)
      throw new Error("error metadata " + error)
    }
  }



}


export default new DataService()
