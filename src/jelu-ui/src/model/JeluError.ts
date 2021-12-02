export class JeluError extends Error{

    constructor(message: string) {
        super(message);
        this.message = message;
      }
}