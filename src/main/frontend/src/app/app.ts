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
  displayedColumns = ['node', 'address', 'tasks', 'lastBeat', 'tripTime'];
  sub?: Subscription;

  constructor(private appService: AppService, private dialog: MatDialog) {}

  setCluster(cluster: Object) {
      if(cluster) {
        this.cluster = Object.entries(cluster).map(([node, info]) => ({ node, ...(info as any) })).sort((a, b) => a.node.localeCompare(b.node));
      } else {
        this.cluster = [];
      }
  }

  ngOnInit() {
    this.appService.connect();
    this.appService.onMessage().subscribe(msg => {
      const cluster = JSON.parse(msg);
      this.setCluster(cluster);
    });
    this.sub = this.appService.poll(5000).subscribe((cluster) => {
      this.setCluster(cluster);
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
