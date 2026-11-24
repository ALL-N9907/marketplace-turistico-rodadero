import {describe,it,expect} from 'vitest';
import {sandboxUrl} from './Payments';
describe('redirección de checkout',()=>{
  it('permite exclusivamente hosts HTTPS sandbox conocidos',()=>{
    expect(sandboxUrl('https://sandbox.mercadopago.com.co/checkout/v1/redirect?pref_id=1')).toBe(true);
    for(const url of ['javascript:alert(1)','https://www.mercadopago.com.co/checkout','http://sandbox.mercadopago.com.co','https://sandbox.mercadopago.com.co.evil.test','https://user:pass@sandbox.mercadopago.com.co'])expect(sandboxUrl(url)).toBe(false);
  });
});
