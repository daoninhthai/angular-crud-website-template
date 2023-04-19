import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { UsersService } from '../../users.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-create',
  templateUrl: './create.component.html',
  styleUrls: ['./create.component.scss']
})
export class CreateComponent implements OnInit {

  form: FormGroup;
   
  constructor(
    public usersService: UsersService,
    private router: Router
  ) { }
  
  ngOnInit(): void {
    this.form = new FormGroup({
      first_name: new FormControl('', [Validators.required]),
      last_name: new FormControl('', [Validators.required]),
      dob: new FormControl('', [Validators.required]),
       gender: new FormControl('', [Validators.required]),
       joined_date: new FormControl('', [Validators.required]),
       authority: new FormControl('',[Validators.required]),
       location: new FormControl('',[Validators.required])
       
     
    });
  }
   
  get f(){
    return this.form.controls;
  }
    
  submit(){
    console.log(this.form.value);
    this.usersService.create(this.form.value).subscribe(res => {
         console.log(this.form.value);
         
         this.router.navigate(["users"]);
    })
  }

}
