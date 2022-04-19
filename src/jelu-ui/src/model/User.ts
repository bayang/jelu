export interface User {
    id?: string,
    creationDate?: string,
    login: string,
    isAdmin: boolean,
    modificationDate?: string
  }
export interface UserAuthentication {
  user: User,
  token?: string
}
export interface CreateUser {
  login: string,
  password: string,
  isAdmin: boolean
}
export interface UpdateUser {
  password: string,
  isAdmin?: boolean
}
