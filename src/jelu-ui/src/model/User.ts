export interface User {
    id?: string,
    creationDate?: string,
    login: string,
    isAdmin: boolean,
    modificationDate?: string,
    provider?: Provider
  }
export interface UserAuthentication {
  user: User,
  token?: string
}
export interface CreateUser {
  login: string,
  password: string,
  isAdmin: boolean,
  provider?: Provider
}
export interface UpdateUser {
  password: string,
  isAdmin?: boolean,
  provider?: Provider
}
export enum Provider {
  LDAP = 'LDAP',
  JELU_DB = 'JELU_DB',
  PROXY = 'PROXY'
}
export interface LoginHistoryInfo {
  ip?: string,
  userAgent?: string,
  source?: string,
  date?: string,
}
