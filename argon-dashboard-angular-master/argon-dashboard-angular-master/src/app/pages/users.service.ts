import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
   
import {  Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { baseUrl } from 'src/environments/environment';
import { UsersComponent } from './users/users.component';
@Injectable({
  providedIn: 'root'
})
export class UsersService {

  headers_object = new HttpHeaders({'Content-Type': 'application/json',"Authorization": "Bearer " + localStorage.getItem('token')});
  
  httpOptions = {
    
    headers: this.headers_object
  }
   
  constructor(private httpClient: HttpClient) { }
     
  getAll(): Observable<any> {

    return this.httpClient.get(`${baseUrl}users`)

    .pipe(
      catchError(this.errorHandler)
    )
  }
     
  create(users:UsersComponent): Observable<any> {

    return this.httpClient.post(`${baseUrl}users/`, JSON.stringify(users), {headers: this.headers_object,
      responseType: "json"})

    .pipe(
      catchError(this.errorHandler)
    )
  }  
     
  find(id:number): Observable<any> {
    console.log(123);
    
    return this.httpClient.get(`${baseUrl}users/` + id)

    .pipe(
      catchError(this.errorHandler)
    )
  }
     
  update(id:number, users:UsersComponent): Observable<any> {

    return this.httpClient.put(`${baseUrl}users/`  + id, JSON.stringify(users), {headers: this.headers_object,
      responseType: "json"})

    .pipe(
      catchError(this.errorHandler)
    )
  }
     
 
  delete(id:number){
    return this.httpClient.delete(`${baseUrl}users/` + id, {headers: this.headers_object,
      responseType: "json"})

    .pipe(
      catchError(this.errorHandler)
    )
  }
    
    
  errorHandler(error:any) {
    let errorMessage = '';
    if(error.error instanceof ErrorEvent) {
      errorMessage = error.error.message;
    } else {
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
    }
    return throwError(errorMessage);
 }
}
