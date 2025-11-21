import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable, switchMap, timer } from "rxjs";

@Injectable({ providedIn: 'root' })
export class AppService {
  constructor(private http: HttpClient) {}

  poll(intervalMs: number): Observable<any> {
    const request = { cmd: 'cluster' };
    return timer(0, intervalMs).pipe(
      switchMap(() => this.http.post('/api', request))
    );
  }

  perform(row: any, request: any) {
    request.node = row.node;
    return this.http.post('/api', request);
  }
}
