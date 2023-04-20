import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { AuthServiceService } from 'src/app/auth-service.service';
import { Router } from '@angular/router';
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {
  formGroup!: FormGroup;
  returnUrl: any;

  constructor(private authService:AuthServiceService,private router: Router) { }

  ngOnInit(){ 
    this.initForm();
  }
  
  initForm(){
    this.formGroup = new FormGroup({
      username: new FormControl('',[Validators.required]),
      password: new FormControl('',[Validators.required]),
    })
  }
  ngOnDestroy() {
  }
  loginProcess(){
    if(this.formGroup.valid){
      this.authService.login(this.formGroup.value).subscribe(result=>{
        if(result){
          console.log(result);
          
          localStorage.setItem ('token', result.jwttoken);
          this.router.navigate(["users"]);
        }
        else{
          console.log('fail');
        }
      })
    }
  }
}
