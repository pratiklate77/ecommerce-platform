import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const money = (value, currency = 'USD') => new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(Number(value || 0));

async function request(path, options = {}) {
  const token = localStorage.getItem('ecommerce_token');
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  if (response.status === 204) return null;
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message || body.error || 'Something went wrong. Please try again.');
  return body;
}

function App() {
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState(() => JSON.parse(localStorage.getItem('ecommerce_cart') || '[]'));
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('ecommerce_user') || 'null'));
  const [view, setView] = useState('shop');
  const [query, setQuery] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => { loadProducts(); }, []);
  useEffect(() => localStorage.setItem('ecommerce_cart', JSON.stringify(cart)), [cart]);

  async function loadProducts(search = '') {
    try { setProducts(await request(`/api/v1/products${search ? `?q=${encodeURIComponent(search)}` : ''}`)); }
    catch (error) { setNotice(error.message); }
  }
  function addToCart(product) {
    setCart(current => {
      const existing = current.find(item => item.id === product.id);
      return existing ? current.map(item => item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item) : [...current, { ...product, quantity: 1 }];
    });
    setNotice(`${product.name} added to your cart.`);
  }
  function updateQuantity(id, quantity) { setCart(current => current.flatMap(item => item.id === id && quantity < 1 ? [] : item.id === id ? [{ ...item, quantity }] : [item])); }
  function signOut() { localStorage.removeItem('ecommerce_token'); localStorage.removeItem('ecommerce_user'); setUser(null); setView('shop'); setNotice('You have been signed out.'); }
  const itemCount = cart.reduce((total, item) => total + item.quantity, 0);

  return <div className="app-shell">
    <header><button className="brand" onClick={() => setView('shop')}>Northstar <span>Market</span></button><nav>
      <button onClick={() => setView('shop')}>Shop</button>
      {user && <button onClick={() => setView('orders')}>Orders</button>}
      {user ? <button onClick={signOut}>Sign out</button> : <button onClick={() => setView('auth')}>Sign in</button>}
      <button className="cart-button" onClick={() => setView('cart')}>Cart <b>{itemCount}</b></button>
    </nav></header>
    {notice && <div className="notice" role="status">{notice}<button onClick={() => setNotice('')}>×</button></div>}
    <main>
      {view === 'shop' && <Shop products={products} query={query} setQuery={setQuery} search={() => loadProducts(query)} addToCart={addToCart} />}
      {view === 'cart' && <Cart cart={cart} updateQuantity={updateQuantity} onCheckout={() => setView(user ? 'checkout' : 'auth')} />}
      {view === 'auth' && <Auth onAuthenticated={(session) => { localStorage.setItem('ecommerce_token', session.token); localStorage.setItem('ecommerce_user', JSON.stringify(session.user)); setUser(session.user); setView(cart.length ? 'checkout' : 'shop'); setNotice(`Welcome, ${session.user.firstName || session.user.email}!`); }} setNotice={setNotice} />}
      {view === 'checkout' && <Checkout cart={cart} clearCart={() => setCart([])} onDone={() => setView('orders')} setNotice={setNotice} />}
      {view === 'orders' && <Orders />}
    </main>
    <footer>Northstar Market · Built on the Ecommerce Platform</footer>
  </div>;
}

function Shop({ products, query, setQuery, search, addToCart }) {
  return <><section className="hero"><p>CURATED FOR EVERYDAY</p><h1>Useful things,<br />beautifully simple.</h1><span>Discover products from independent makers and reliable favorites.</span></section>
  <section className="toolbar"><h2>Shop all</h2><form onSubmit={event => { event.preventDefault(); search(); }}><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search products" /><button>Search</button></form></section>
  <section className="grid">{products.map(product => <article className="product" key={product.id}><div className="product-image">{product.imageUrl ? <img src={product.imageUrl} alt="" /> : <span>{product.category?.slice(0, 1)}</span>}</div><p>{product.category}</p><h3>{product.name}</h3><div><strong>{money(product.price, product.currency)}</strong><button onClick={() => addToCart(product)}>Add</button></div></article>)}{!products.length && <p className="empty">No products found yet. Add products through the catalog API, then refresh this page.</p>}</section></>;
}

function Cart({ cart, updateQuantity, onCheckout }) {
  const total = useMemo(() => cart.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0), [cart]);
  return <section className="panel"><h1>Your cart</h1>{!cart.length ? <p className="empty">Your cart is waiting for something good.</p> : <><div className="cart-list">{cart.map(item => <div className="cart-row" key={item.id}><div><h3>{item.name}</h3><p>{money(item.price, item.currency)}</p></div><div className="quantity"><button onClick={() => updateQuantity(item.id, item.quantity - 1)}>−</button><span>{item.quantity}</span><button onClick={() => updateQuantity(item.id, item.quantity + 1)}>+</button></div><strong>{money(Number(item.price) * item.quantity, item.currency)}</strong></div>)}</div><div className="summary"><span>Subtotal</span><strong>{money(total, cart[0]?.currency)}</strong><button className="primary" onClick={onCheckout}>Checkout</button></div></>}</section>;
}

function Auth({ onAuthenticated, setNotice }) {
  const [mode, setMode] = useState('login'); const [form, setForm] = useState({ email: '', password: '', firstName: '', lastName: '' }); const [busy, setBusy] = useState(false);
  const submit = async event => { event.preventDefault(); setBusy(true); try { if (mode === 'register') { await request('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(form) }); setMode('login'); setNotice('Account created. Sign in to continue.'); } else onAuthenticated(await request('/api/v1/auth/login', { method: 'POST', body: JSON.stringify({ email: form.email, password: form.password }) })); } catch (error) { setNotice(error.message); } finally { setBusy(false); } };
  return <section className="auth panel"><p className="eyebrow">{mode === 'login' ? 'WELCOME BACK' : 'JOIN NORTHSTAR'}</p><h1>{mode === 'login' ? 'Sign in' : 'Create your account'}</h1><form onSubmit={submit}>{mode === 'register' && <div className="form-row"><input required placeholder="First name" onChange={e => setForm({ ...form, firstName: e.target.value })}/><input required placeholder="Last name" onChange={e => setForm({ ...form, lastName: e.target.value })}/></div>}<input required type="email" placeholder="Email address" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}/><input required minLength="8" type="password" placeholder="Password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })}/><button className="primary" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}</button></form><button className="text-button" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>{mode === 'login' ? 'New here? Create an account' : 'Already have an account? Sign in'}</button></section>;
}

function Checkout({ cart, clearCart, onDone, setNotice }) {
  const [form, setForm] = useState({ line1: '', city: '', state: '', postalCode: '', country: '', paymentToken: 'tok_web_demo' }); const [busy, setBusy] = useState(false); const total = cart.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0);
  const submit = async event => { event.preventDefault(); setBusy(true); try { const order = await request('/api/v1/orders', { method: 'POST', body: JSON.stringify({ currency: cart[0]?.currency || 'USD', shippingAddress: form, shippingCost: 0, taxAmount: 0, items: cart.map(item => ({ productId: item.id, productName: item.name, sku: item.sku, unitPrice: item.price, quantity: item.quantity })) }) }); await request('/api/v1/payments', { method: 'POST', body: JSON.stringify({ orderId: order.id, orderNumber: order.orderNumber, amount: order.totalAmount, currency: order.currency, method: 'CARD', paymentToken: form.paymentToken }) }); clearCart(); setNotice(`Order ${order.orderNumber} is confirmed.`); onDone(); } catch (error) { setNotice(error.message); } finally { setBusy(false); } };
  if (!cart.length) return <section className="panel"><h1>Your cart is empty</h1></section>;
  return <section className="checkout panel"><div><p className="eyebrow">SECURE CHECKOUT</p><h1>Where should we send it?</h1><form onSubmit={submit}><input required placeholder="Address line" onChange={e => setForm({ ...form, line1: e.target.value })}/><div className="form-row"><input required placeholder="City" onChange={e => setForm({ ...form, city: e.target.value })}/><input placeholder="State / region" onChange={e => setForm({ ...form, state: e.target.value })}/></div><div className="form-row"><input placeholder="Postal code" onChange={e => setForm({ ...form, postalCode: e.target.value })}/><input required placeholder="Country" onChange={e => setForm({ ...form, country: e.target.value })}/></div><input required placeholder="Payment token" value={form.paymentToken} onChange={e => setForm({ ...form, paymentToken: e.target.value })}/><button className="primary" disabled={busy}>{busy ? 'Processing…' : `Pay ${money(total, cart[0]?.currency)}`}</button></form></div><aside><h3>Order summary</h3>{cart.map(item => <p key={item.id}>{item.quantity} × {item.name}<strong>{money(Number(item.price) * item.quantity, item.currency)}</strong></p>)}<hr/><p>Total <strong>{money(total, cart[0]?.currency)}</strong></p></aside></section>;
}

function Orders() { const [orders, setOrders] = useState([]); const [error, setError] = useState(''); useEffect(() => { request('/api/v1/orders').then(setOrders).catch(e => setError(e.message)); }, []); return <section className="panel"><p className="eyebrow">YOUR ACCOUNT</p><h1>Order history</h1>{error && <p className="error">{error}</p>}{!orders.length && !error ? <p className="empty">No orders yet.</p> : <div className="orders">{orders.map(order => <article key={order.id}><div><h3>{order.orderNumber}</h3><p>{new Date(order.createdAt).toLocaleDateString()} · {order.items.length} item(s)</p></div><span className="status">{order.status}</span><strong>{money(order.totalAmount, order.currency)}</strong></article>)}</div>}</section>; }

createRoot(document.getElementById('root')).render(<App />);
