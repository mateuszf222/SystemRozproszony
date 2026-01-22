import { Component, Inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';

import { AppService } from './app.service';

export function jsonValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;

  try {
    JSON.parse(control.value);
    return null;
  } catch {
    return { invalidJson: true };
  }
}

@Component({
  selector: 'apidialog',
  standalone: true,
  imports: [ CommonModule, MatDialogModule, MatInputModule, MatSelectModule, ReactiveFormsModule ],
  templateUrl: './app.apidialog.html',
  styleUrls: ['./app.apidialog.scss']
})
export class ApiDialog {
    form: FormGroup;
    formValid: boolean = false;
    response = "";
    nodes: any[] = [];

    constructor(
        private snackBar: MatSnackBar,
        private dialogRef: MatDialogRef<ApiDialog>,
        private fb: FormBuilder,
        private appService: AppService,
        @Inject(MAT_DIALOG_DATA) public data: { row: any, cluster: any[] }
    ) {
      if (data && data.cluster) {
          this.nodes = data.cluster;
      }
      this.form = this.fb.group({
        node: [data.row.node, Validators.required],
        cmd: ['sleep', Validators.required],
        args: ['{"ms":10000}', jsonValidator]
      });
    }

    onPerform(): void {
      if (this.form.valid) {
        // We override 'row' temporarily to contain the selected node from dropdown
        const targetRow = { ...this.data.row, node: this.form.value.node };
        
        this.appService.perform(targetRow, this.form.value).subscribe({
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