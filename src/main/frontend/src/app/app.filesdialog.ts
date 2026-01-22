import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { AppService } from './app.service';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';

@Component({
  selector: 'files-dialog',
  templateUrl: './app.filesdialog.html',
  styleUrls: ['./app.filesdialog.scss'],
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, FormsModule, MatTableModule, MatIconModule, MatSelectModule]
})
export class FilesDialog implements OnInit {
  files: any[] = [];
  displayedColumns = ['name', 'size', 'actions'];
  cluster: any[] = [];
  
  constructor(
    public dialogRef: MatDialogRef<FilesDialog>,
    private service: AppService,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    if (data && data.cluster) {
        // We want to transfer to OTHER nodes. If 'me' is not set properly in data, we can filter by logic.
        // Assuming cluster data has 'me' boolean or we can assume we transfer to others.
        // 'data.cluster' comes from app.ts which maps object to array.
        // Let's filter out self if 'me' is true.
        this.cluster = data.cluster.filter((n: any) => !n.me);
    }
  }

  ngOnInit() {
    this.refreshFiles();
  }

  refreshFiles() {
    this.service.getFiles().subscribe(files => {
      this.files = files;
    });
  }

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      this.service.uploadFile(file).subscribe({
        next: () => {
             this.refreshFiles();
             event.target.value = ''; // Reset input
        },
        error: (err) => alert('Upload failed: ' + err.message)
      });
    }
  }

  download(filename: string) {
    this.service.downloadFile(filename);
  }

  transfer(filename: string, targetNode: string) {
    if (!targetNode) return;
    this.service.transferFile(filename, targetNode).subscribe({
        next: (res) => alert(res),
        error: (err) => alert('Transfer failed: ' + err.message)
    });
  }
}
