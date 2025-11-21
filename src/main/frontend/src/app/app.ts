import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { Subscription } from 'rxjs';

import { AppService } from './app.service';
import { ApiDialog } from './app.apidialog';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
  standalone: true,
  imports: [CommonModule, MatToolbarModule, MatTableModule, MatDialogModule]
})
export class App {
  protected title = 'PUS2025';

  cluster: any[] = [];
  displayedColumns = ['node', 'address', 'lastBeat', 'tripTime'];
  sub?: Subscription;
  
  constructor(private appService: AppService, private dialog: MatDialog) {}

  ngOnInit() {
    this.sub = this.appService.poll(5000).subscribe((clusterResponse) => {
      if(clusterResponse?.result) {
        this.cluster = Object.entries(clusterResponse.result).map(([node, info]) => ({ node, ...(info as any) }));
      } else {
        this.cluster = [];
      }
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  openDialog(row: any) {
    this.dialog.open(ApiDialog, {
      width: '50%',
      data: { row }
    });
  }
}
