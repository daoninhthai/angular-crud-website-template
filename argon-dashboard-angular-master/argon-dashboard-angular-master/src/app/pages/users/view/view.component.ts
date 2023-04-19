import { Users } from './../../users';
import { Component, OnInit } from '@angular/core';
import { UsersService } from '../../users.service';
import { ActivatedRoute, Router } from '@angular/router';

  
@Component({
  selector: 'app-view',
  templateUrl: './view.component.html',
  styleUrls: ['./view.component.scss']
})
export class ViewComponent implements OnInit {
   
  id: number;
  user: Users;
   
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
  }
  
}