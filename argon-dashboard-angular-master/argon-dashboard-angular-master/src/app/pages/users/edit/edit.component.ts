import { Users } from './../../users';
import { Component, OnInit } from '@angular/core';
import { UsersService } from '../../users.service';
import { ActivatedRoute, Router } from '@angular/router';
import { FormGroup, FormControl, Validators} from '@angular/forms';
import Swal from 'sweetalert2/dist/sweetalert2.js';
@Component({
  selector: 'app-edit',
  templateUrl: './edit.component.html',
  styleUrls: ['./edit.component.scss']
})
export class EditComponent implements OnInit {
    
  id: number;
  user: Users;
  form: FormGroup;
  
  constructor(
    public usersService: UsersService,
    private route: ActivatedRoute,
    private router: Router
  ) { }
  
  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.usersService.find(this.id).subscribe((data: Users)=>{
      this.user = data;
    });
    
    this.form = new FormGroup({
      username: new FormControl('', [Validators.required]),
      first_name: new FormControl('', [Validators.required]),
      last_name: new FormControl('', [Validators.required]),
      dob: new FormControl('', [Validators.required]),
       location: new FormControl('', [Validators.required]),
       gender: new FormControl('', [Validators.required]),
       joined_date: new FormControl('', [Validators.required]),
       status: new FormControl(''),
       password: new FormControl(''),
       authority: new FormControl(''),
       staff_code: new FormControl(''),
       default_password: new FormControl(''),
       first_login: new FormControl(''),
    });
  }
   
  get f(){
    return this.form.controls;
  }
  errorAlert(title: string, text: string) {
    Swal.fire({
      title: title,
      text: text,
      icon: 'error',
      showCancelButton: false,
      showConfirmButton: false,
      timer: 2000
    })
  }
  successAlert(title: string, text: string) {
    Swal.fire({
      title: title,
      text: text,
      icon: 'success',
      showCancelButton: false,
      showConfirmButton: false,
      timer: 2000
    })
  }
  submit(){
    console.log(this.form.value);
    this.usersService.update(this.id, this.form.value).subscribe( {
      next: (res) => {
        this.successAlert('Create success', 'Your Staff has been updated!')
        this.router.navigate(["users"]);
      },
         error: (error) => {
      console.log(error)
      this.errorAlert('Create fail!', 'Your Staff can not be update!') }
    })
    
   
   
    
  }
   
}