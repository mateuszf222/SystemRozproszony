import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { AppService } from './app.service';

@Component({
  selector: 'logsdialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatTabsModule, MatTableModule, MatButtonModule],
  templateUrl: './app.logsdialog.html',
  styleUrls: ['./app.logsdialog.scss']
})
export class LogsDialog implements OnInit {
    executionLogs: any[] = [];
    communicationLogs: any[] = [];
    
    executionColumns = ['id', 'timestamp', 'cmd', 'args', 'execution_time', 'result', 'description'];
    communicationColumns = ['id', 'timestamp', 'type', 'request', 'response'];

    constructor(
        private dialogRef: MatDialogRef<LogsDialog>,
        private appService: AppService
    ) {}

    ngOnInit() {
        this.refresh();
    }

    refresh() {
        this.appService.getExecutionLogs().subscribe(logs => this.executionLogs = logs);
        this.appService.getCommunicationLogs().subscribe(logs => this.communicationLogs = logs);
    }
}
