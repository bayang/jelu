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
