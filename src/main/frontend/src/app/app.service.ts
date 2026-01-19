import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable, Subject, switchMap, timer } from "rxjs";

@Injectable({ providedIn: 'root' })
export class AppService {
  private ws?: WebSocket;
  private messages$ = new Subject<string>();

  constructor(private http: HttpClient) {}

  poll(intervalMs: number): Observable<any> {
    return timer(0, intervalMs).pipe(
      switchMap(() => this.http.get('/api'))
    );
  }

  perform(row: any, request: any) {
    try {
      request.args = JSON.parse(request.args);
      request.node = row.node;
    } catch(ex) {}
    return this.http.post('/api', request);
  }

  join(url: string) {
    return this.http.put('/api', { url }, { responseType: 'text' });
  }

  connect(): void {
    this.ws = new WebSocket('/ws');

    this.ws.onopen = () => {};
    this.ws.onmessage = event => {
      this.messages$.next(event.data);
    };
    this.ws.onclose = () => { this.connect(); };
    this.ws.onerror = err => {
      console.error('WebSocket error, trying to reconnect', err);
      this.connect();
    };
  }

  onMessage() {
    return this.messages$.asObservable();
  }

  send(msg: string) {
    this.ws?.send(msg);
  }
}
