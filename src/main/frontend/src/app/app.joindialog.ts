import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';

import { AppService } from './app.service';

@Component({
  selector: 'joindialog',
  standalone: true,
  imports: [ CommonModule, MatDialogModule, MatInputModule, MatButtonModule, ReactiveFormsModule ],
  templateUrl: './app.joindialog.html',
  styleUrls: ['./app.joindialog.scss']
})
export class JoinDialog {
    form: FormGroup;
    response = "";

    constructor(
        private snackBar: MatSnackBar,
        private dialogRef: MatDialogRef<JoinDialog>,
        private fb: FormBuilder,
        private appService: AppService
    ) {
      this.form = this.fb.group({
        url: ['', Validators.required]
      });
    }

    onJoin(): void {
      if (this.form.valid) {
        this.appService.join(this.form.value.url).subscribe({
          next: (res: string) => {
            this.response = res;
            this.snackBar.open('Join success', 'Close', {
              duration: 5000,
              panelClass: ['snackbar-success']
            });
            // Optional: Close dialog on success after a delay or immediately
            // this.dialogRef.close();
          },
          error: (err: any) => {
            this.response = "Error: " + (err.error || err.message);
            this.snackBar.open('Join failed', 'Close', {
              duration: 5000,
              panelClass: ['snackbar-error']
            });
          }
        });
      }
    }
}
