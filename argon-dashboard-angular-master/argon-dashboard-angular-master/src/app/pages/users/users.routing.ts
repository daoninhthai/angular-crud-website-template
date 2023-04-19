import {  Routes } from '@angular/router';
import { ViewComponent } from './view/view.component';
import { CreateComponent } from './create/create.component';
import { EditComponent } from './edit/edit.component';
import { UsersComponent } from './users.component';


export const usersRouter : Routes = [
  { path: '', redirectTo: 'users', pathMatch: 'full'},
  
  { path: 'users', component: UsersComponent, children:[
    { path: ':id/view', component: ViewComponent },
  ] },
  
  { path: 'users/create', component: CreateComponent },
  { path: 'users/:id/edit', component: EditComponent }
];

