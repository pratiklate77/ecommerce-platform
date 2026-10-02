import http from 'k6/http';
import { check } from 'k6';

export const options = {

    vus: 10000,
    duration: '10s',
};

export default function () {


    const response = http.get('http://localhost:8081/api/v1/auth/getuser/6');

    check(response, {
        'status is 200': (r) => r.status === 200,
    });
}
