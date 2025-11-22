import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable, Subject, switchMap, timer } from "rxjs";

@Injectable({ providedIn: 'root' })
export class AppService {
  private ws?: WebSocket;
  private messages$ = new Subject<string>();
  
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

  connect(): void {
    this.ws = new WebSocket('/ws');
    console.log('Connect');

    this.ws.onopen = () => {
      console.log('WebSocket connected');
    };

    this.ws.onmessage = event => {
      this.messages$.next(event.data);
    };

    this.ws.onclose = () => {
      console.log('WebSocket disconnected');
    };

    this.ws.onerror = err => {
      console.error('WebSocket error', err);
    };
  }

  onMessage() {
    return this.messages$.asObservable();
  }

  send(msg: string) {
    this.ws?.send(msg);
  }
}
