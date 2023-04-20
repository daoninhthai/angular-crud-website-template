import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { ViewComponent } from './view/view.component';
import { CreateComponent } from './create/create.component';
import { EditComponent } from './edit/edit.component';
  
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { usersRouter } from './users.routing';
import { UploadComponent } from './upload/upload.component';


  
@NgModule({
  declarations: [ ViewComponent, CreateComponent, EditComponent, UploadComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forChild(usersRouter),
  ]
})
export class UsersModule { }