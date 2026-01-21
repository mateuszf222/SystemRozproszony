import { Component, OnInit, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
import { AppService } from './app.service';

@Component({
  selector: 'logsdialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatTabsModule, MatTableModule, MatButtonModule, MatSelectModule, MatFormFieldModule, FormsModule],
  templateUrl: './app.logsdialog.html',
  styleUrls: ['./app.logsdialog.scss']
})
export class LogsDialog implements OnInit {
    executionLogs: any[] = [];
    communicationLogs: any[] = [];
    nodes: any[] = [];
    selectedNode: string = "";
    
    executionColumns = ['id', 'timestamp', 'cmd', 'args', 'execution_time', 'result', 'description'];
    communicationColumns = ['id', 'timestamp', 'type', 'request', 'response'];

    constructor(
        private dialogRef: MatDialogRef<LogsDialog>,
        private appService: AppService,
        @Inject(MAT_DIALOG_DATA) public data: { cluster: any[] }
    ) {
        if (data && data.cluster) {
            this.nodes = data.cluster;
        }
    }

    ngOnInit() {
        // Set default to 'me'
        const me = this.nodes.find(n => n.me);
        if (me) {
            this.selectedNode = me.node;
        } else if (this.nodes.length > 0) {
            this.selectedNode = this.nodes[0].node;
        }
        this.refresh();
    }

    refresh() {
        this.appService.getExecutionLogs(this.selectedNode).subscribe(logs => this.executionLogs = logs);
        this.appService.getCommunicationLogs(this.selectedNode).subscribe(logs => this.communicationLogs = logs);
    }
}
