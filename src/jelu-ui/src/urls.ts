class Urls {

    public MODE: string;

    public BASE_URL: string;

    public API_URL: string

    constructor() {
        if (import.meta.env.DEV) {
            this.MODE = "dev"
            this.BASE_URL = import.meta.env.VITE_API_URL as string
            this.API_URL = this.BASE_URL
        }
        else {
            this.MODE = "prod"
            this.BASE_URL = window.location.origin.endsWith("/") ? window.location.origin.slice(0, -1) : window.location.origin
            this.API_URL = this.BASE_URL + "/api/v1"
        }
        console.log(`running in ${this.MODE} mode at ${this.BASE_URL} and ${this.API_URL}`)
    }

}
export default new Urls()
