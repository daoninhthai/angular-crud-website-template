import { Component, OnInit } from '@angular/core';
import { UsersService } from '../users.service';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {

  users = [];
  constructor(public usersService: UsersService) { }

  ngOnInit(): void {
    this.usersService.getAll().subscribe((data: any)=>{
      this.users = data;
      console.log(this.users);
    })  
  }
  // deleteUser(id:number){
  //   this.usersService.delete(id).subscribe(res => {
  //        this.users = this.users.filter(item => item.id !== id);
  //        console.log('User deleted successfully!');
  //   })
  // }

}
