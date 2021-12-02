import axios, { AxiosError, AxiosInstance } from "axios";
import Book from "../model/Book";
import router from '../router'
import { User, UserAuthentication } from "../model/User";
import { JeluError } from "../model/JeluError";


class DataService {

  private apiClient: AxiosInstance;
  
  private token?: string = ''

  private TOKEN_KEY: string = 'jelu-token'

  private API_BOOK = '/books';
  
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

  findAll = async () => {
    try {
      const response = await this.apiClient.get<Array<Book>>("/books/me");
      console.log("called backend")
      console.log(response)
      return response.data;
    }
    catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        console.log("error axios " + error.response.status + " " + error.response.data.error)
        // await router.push({ name: 'home', params: {msg: 'msg'}})

      }
      console.log("error findall " + (error as AxiosError).toJSON())
      console.log("error findall " + (error as AxiosError).code)
      throw new Error("error findall " + error)
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

      // return response.data;
      
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
          'email' : login,
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



}


export default new DataService()
