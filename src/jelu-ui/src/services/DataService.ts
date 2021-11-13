import axios, { AxiosError } from "axios";
import Book from "../model/Book";

const apiClient = axios.create({
    baseURL: "http://localhost:11111/api",
    headers: {
      "Content-type": "application/json",
    }
  });

const findAll = async () => {
  try {
    const response = await apiClient.get<Array<Book>>("/books/me");
    console.log("called backend")
    console.log(response)
    return response.data;
  } 
  catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      console.log("error axios " + error.response.status+ " " + error.response.data.error)
    }
    console.log("error findall " + (error as AxiosError).toJSON())
    console.log("error findall " + (error as AxiosError).code)
    throw new Error("error findall " + error)
  }
  }  

const DataService = {
    findAll
}

export default DataService
