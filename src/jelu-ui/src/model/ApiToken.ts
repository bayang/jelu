export interface ApiToken {
    id: string,
    userId: string,
    name: string,
    scopes: string[],
    createdAt: string,
    lastUsedAt?: string,
    usageCount: number,
    expiresAt?: string,
    isActive: boolean
}

export interface CreateApiToken {
    name: string,
    scopes: string[],
    expiresAt?: string
}

export interface UpdateApiToken {
    name?: string,
    scopes?: string[],
    isActive?: boolean
}

export interface ApiTokenCreated {
    token: ApiToken,
    rawToken: string
}

export interface TokenScope {
    name: string,
    description: string,
    category: string
}

export interface AdminApiToken {
    id: string,
    userId: string,
    userLogin: string,
    name: string,
    scopes: string[],
    createdAt: string,
    lastUsedAt?: string,
    usageCount: number,
    expiresAt?: string,
    isActive: boolean
}
