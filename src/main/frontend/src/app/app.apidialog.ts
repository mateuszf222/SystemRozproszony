import { Component, Inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AppService } from './app.service';

@Component({
  selector: 'apidialog',
  standalone: true,
  imports: [ MatDialogModule, MatInputModule, ReactiveFormsModule ],
  templateUrl: './app.apidialog.html',
  styleUrls: ['./app.apidialog.scss']
})
export class ApiDialog {
    form: FormGroup;
    formValid: boolean = false;
    response = "";

    constructor(
        private snackBar: MatSnackBar,
        private dialogRef: MatDialogRef<ApiDialog>,
        private fb: FormBuilder,
        private appService: AppService,
        @Inject(MAT_DIALOG_DATA) public data: { row: any }
    ) {
      this.form = this.fb.group({
        cmd: ['', Validators.required],
        args: ['']
      });
    }

    onPerform(): void {
      if (this.form.valid) {
        this.appService.perform(this.data.row, this.form.value).subscribe({
          next: (response: any) => {
            this.response = JSON.stringify(response.result);
            this.snackBar.open('Perform successed', 'Close', {
              duration: 5000,
              panelClass: ['snackbar-success']
            });
          },
          error: (_err: any) => {
            this.snackBar.open('Perform failed', 'Close', {
              duration: 5000,
              panelClass: ['snackbar-error']
            });
          }
        });
      }
    }
}