export enum MetadataErrorType {
    EXIT_CODE_NOT_ZERO = 'EXIT_CODE_NOT_ZERO',
    EXCEPTION_CAUGHT = 'EXCEPTION_CAUGHT',
}
export interface MetadataError {
    sourcePlugin: string,
    errorType: MetadataErrorType,
    pluginErrorMessage?: string,
}
