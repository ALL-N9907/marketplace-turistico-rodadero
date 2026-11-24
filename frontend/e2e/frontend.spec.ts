import { test, expect, type Page } from '@playwright/test';
async function mockSession(page:Page,identity:unknown){
  let loggedIn=false;
  await page.route('**/api/auth/me',route=>route.fulfill(loggedIn?{json:identity}:{status:401,json:{detail:'Sin sesión'}}));
  await page.route('**/api/auth/login',async route=>{loggedIn=true;await route.fulfill({json:identity});});
  await page.route('**/api/auth/logout',async route=>{loggedIn=false;await route.fulfill({status:204});});
}
// Dobles HTTP sólo en pruebas: nunca se incluyen en la aplicación.
const tour={id:1,agenciaId:1,tipoExperienciaId:1,titulo:'Caminata a Minca',descripcion:'Una caminata entre cafetales.',destino:'Minca',precio:180000,duracionHoras:8,nivelDificultad:'MEDIO',serviciosIncluidos:'Guía',estado:'ACTIVE'};
test.beforeEach(async({page})=>{
  await page.route('**/api/**',async route=>{
    const u=new URL(route.request().url()),p=u.pathname;
    let data:unknown=[];
    if(p==='/api/auth/me'){await route.fulfill({status:401,json:{detail:'Sin sesión'}});return;}
    if(p==='/api/auth/csrf'){await route.fulfill({json:{headerName:'X-CSRF-TOKEN',token:'test-token'}});return;}
    if(p==='/api/catalog/tours')data={content:[{tour,details:{supportsCommunities:true,environmentalPractices:false}}],totalElements:1,page:0,size:9};
    else if(p==='/api/agencies')data=[{id:1,nombreComercial:'Sierra Viva',estado:'ACTIVE'}];
    else if(p==='/api/experience-types')data=[{id:1,nombre:'Naturaleza'}];
    else if(p==='/api/tours/1')data=tour;
    else if(p==='/api/tours/1/details')data={itinerary:'07:00 Salida',conditions:'Llevar agua',supportsCommunities:true,environmentalPractices:false};
    else if(p==='/api/tours/1/reviews')data={content:[],totalElements:0,page:0,size:5,totalPages:0,averageRating:null};
    else if(/^\/api\/reservations\/\d+\/history$/.test(p))data={content:[],totalElements:0,page:0,size:20,totalPages:0};
    else if(p==='/api/tours/1/video'){await route.fulfill({status:404,json:{detail:'Sin video'}});return;}
    else if(p==='/api/agencies/1')data={id:1,nombreComercial:'Sierra Viva'};
    else if(p==='/api/availability/tours/1')data=[{horarioId:3,tourId:1,fechaTour:'2027-01-15',horaInicio:'07:00:00',fechaFin:'2027-01-15T15:00:00',cuposTotales:12,cuposDisponibles:6}];
    await route.fulfill({json:data});
  });
});
test('catálogo navegable y diseño sin desbordamiento horizontal',async({page},testInfo)=>{
  await page.goto('/');
  await expect(page.getByRole('heading',{name:'Menos vueltas. Más aventuras.'})).toBeVisible();
  await expect(page.getByRole('heading',{name:'Caminata a Minca'})).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await page.screenshot({path:testInfo.outputPath('catalogo.png'),fullPage:true});
  await page.getByRole('heading',{name:'Caminata a Minca'}).getByRole('link').click();
  await expect(page.getByRole('heading',{name:'Caminata a Minca',exact:true})).toBeVisible();
});
test('una solicitud sin sesión lleva al acceso, no a un éxito ficticio',async({page})=>{await page.goto('/tours/1');await page.getByLabel('Fecha de salida').fill('2027-01-15');await page.getByRole('button',{name:'Consultar disponibilidad'}).click();await page.getByRole('radio').check();await page.getByRole('button',{name:'Iniciar sesión para reservar'}).click();await expect(page).toHaveURL(/\/acceso$/);});
test('una ruta privada exige autenticación',async({page})=>{await page.goto('/panel/cuentas');await expect(page).toHaveURL(/\/acceso$/);await expect(page.getByLabel('Correo electrónico')).toBeVisible();});
test('un fallo de API se comunica sin datos de muestra',async({page})=>{await page.route('**/api/catalog/tours?**',route=>route.fulfill({status:503,json:{detail:'Base de datos no disponible'}}));await page.goto('/');await expect(page.getByRole('alert')).toContainText('Base de datos no disponible');await expect(page.getByRole('heading',{name:'Caminata a Minca'})).toHaveCount(0);});

test('el turista autenticado solicita una reserva con CSRF y no puede confirmarla',async({page})=>{
  const reservation={id:42,turistaId:7,horarioId:3,fechaReserva:'2026-11-23T10:00:00',fechaInicio:'2027-01-15T07:00:00',fechaFin:'2027-01-15T15:00:00',cantidadPersonas:1,precioTotal:180000,estado:'SCHEDULED',fechaModificacion:'2026-11-23T10:00:00'};
  await page.route('**/api/auth/csrf',route=>route.fulfill({json:{headerName:'X-CSRF-TOKEN',token:'test-token'}}));
  await mockSession(page,{email:'ana@ejemplo.com',role:'TOURIST',touristId:7,agencyId:null,guideId:null});
  await page.route('**/api/reservations',async route=>{
    expect(route.request().method()).toBe('POST');
    expect(route.request().headers()['x-csrf-token']).toBe('test-token');
    expect(route.request().postDataJSON()).toMatchObject({turistaId:7,horarioId:3,cantidadPersonas:1});
    expect(route.request().postDataJSON()).not.toHaveProperty('estado');
    await route.fulfill({status:201,json:reservation});
  });
  await page.route('**/api/reservations/42',route=>route.fulfill({json:reservation}));
  await page.route('**/api/reservations/42/voucher',route=>route.fulfill({json:{reservationId:42,tour:tour.titulo,agency:'Sierra Viva',status:'SCHEDULED'}}));
  await page.route('**/api/reservations/42/history?**',route=>route.fulfill({json:{content:[{id:1,previousStatus:null,status:'SCHEDULED',occurredAt:'2026-11-23T10:00:00',actorRole:'TOURIST',reason:null}],totalElements:1,page:0,size:20,totalPages:1}}));
  await page.goto('/acceso');
  await page.getByLabel('Correo electrónico').fill('ana@ejemplo.com');
  await page.getByLabel('Contraseña',{exact:true}).fill('una-clave-segura');
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
  await expect(page).toHaveURL(/\/reservas$/);
  await page.getByRole('banner').getByRole('link',{name:'Rodatours, inicio'}).click();
  await page.getByRole('heading',{name:'Caminata a Minca'}).getByRole('link').click();
  await page.getByLabel('Fecha de salida').fill('2027-01-15');
  await page.getByRole('button',{name:'Consultar disponibilidad'}).click();
  await page.getByRole('radio').check();
  await page.getByRole('button',{name:'Solicitar reserva',exact:true}).click();
  await expect(page).toHaveURL(/\/reservas\/42$/);
  await expect(page.getByText('Solicitud creada.',{exact:false})).toBeVisible();
  await expect(page.getByRole('button',{name:'Cancelar con motivo'})).toBeVisible();
  await expect(page.getByRole('button',{name:'Confirmar reserva',exact:true})).toHaveCount(0);
  await expect(page.getByRole('heading',{name:'Historial de estados'})).toBeVisible();
  await expect(page.getByText('Reserva creada →',{exact:true})).toBeVisible();
});

test('operador consulta por una salida y no por un listado global',async({page})=>{
  await mockSession(page,{email:'operador@ejemplo.com',role:'AGENCY_OPERATOR',agencyId:2,touristId:null,guideId:null});
  const requests:string[]=[];
  await page.route('**/api/reservations?**',async route=>{requests.push(route.request().url());await route.fulfill({json:[]});});
  await page.goto('/acceso');
  await page.getByLabel('Correo electrónico').fill('operador@ejemplo.com');
  await page.getByLabel('Contraseña',{exact:true}).fill('una-clave-segura');
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
  await expect(page).toHaveURL(/\/panel$/);
  // El enlace del resumen funciona también sin abrir el menú lateral móvil.
  await page.getByRole('heading',{name:'Gestionar reservas'}).click();
  await page.getByLabel('ID de una salida de tu agencia').fill('3');
  await page.getByRole('button',{name:'Consultar reservas'}).click();
  await expect(page.getByRole('heading',{name:'No hay reservas para esta consulta'})).toBeVisible();
  expect(requests.length).toBeGreaterThan(0);
  expect(requests.every(url=>new URL(url).searchParams.get('horarioId')==='3')).toBe(true);
  await expect(page.getByRole('button',{name:'Crear solicitud pendiente'})).toHaveCount(0);
});

test('las reseñas públicas muestran promedio y paginación sin requerir acceso',async({page})=>{
  await page.route('**/api/tours/1/reviews?**',route=>{
    const next=new URL(route.request().url()).searchParams.get('page')==='1';
    return route.fulfill({json:{content:[{id:next?2:1,rating:next?4:5,comment:next?'Buen recorrido':'Excelente experiencia',createdAt:'2026-11-23T12:00:00'}],totalElements:6,page:next?1:0,size:5,totalPages:2,averageRating:4.5}});
  });
  await page.goto('/tours/1');
  await expect(page.getByText('4,5 / 5',{exact:true})).toBeVisible();
  await expect(page.getByText('Excelente experiencia',{exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Más reseñas'}).click();
  await expect(page.getByText('Buen recorrido',{exact:true})).toBeVisible();
  await expect(page.getByText('Excelente experiencia',{exact:true})).toHaveCount(0);
});

test('el guía canjea el QR sin ID y un rechazo posterior elimina el éxito anterior',async({page})=>{
  await mockSession(page,{email:'guia@ejemplo.com',role:'GUIDE',agencyId:2,guideId:3,touristId:null});
  await page.route('**/api/auth/csrf',route=>route.fulfill({json:{headerName:'X-CSRF-TOKEN',token:'test-token'}}));
  let uses=0;
  await page.route('**/api/reservations/check-in',async route=>{
    expect(route.request().postDataJSON()).toEqual({qrPayload:'rodatours:reservation:valid-code'});
    expect(route.request().headers()['x-csrf-token']).toBe('test-token');
    uses++;
    if(uses===1)await route.fulfill({json:{reservationId:42,tour:'Minca',persons:2,checkedInAt:'2026-11-23T08:00:00'}});
    else await route.fulfill({status:409,json:{detail:'El QR ya fue canjeado.'}});
  });
  await page.goto('/acceso');
  await page.getByLabel('Correo electrónico').fill('guia@ejemplo.com');
  await page.getByLabel('Contraseña',{exact:true}).fill('una-clave-segura');
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
  await expect(page).toHaveURL(/\/panel$/);
  await page.getByRole('heading',{name:'Validar acceso'}).click();
  await expect(page.getByLabel('ID de reserva',{exact:true})).toHaveCount(0);
  await page.getByLabel('Contenido del QR').fill('rodatours:reservation:valid-code');
  await page.getByRole('button',{name:'Validar y registrar acceso'}).click();
  await expect(page.getByRole('heading',{name:'Acceso registrado'})).toBeVisible();
  await page.getByRole('button',{name:'Validar y registrar acceso'}).click();
  await expect(page.getByRole('alert')).toContainText('El QR ya fue canjeado.');
  await expect(page.getByRole('heading',{name:'Acceso registrado'})).toHaveCount(0);
});

test('la sesión se restaura al recargar y salir impide volver a una ruta privada',async({page})=>{
  await mockSession(page,{email:'ana@ejemplo.com',role:'TOURIST',touristId:7,agencyId:null,guideId:null});
  await page.goto('/acceso');await page.getByLabel('Correo electrónico').fill('ana@ejemplo.com');
  await page.getByLabel('Contraseña',{exact:true}).fill('una-clave-segura');
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await expect(page).toHaveURL(/\/reservas$/);
  await page.reload();await expect(page.getByRole('heading',{name:'Tus próximas historias'})).toBeVisible();
  expect(await page.evaluate(()=>({local:localStorage.length,session:sessionStorage.length}))).toEqual({local:0,session:0});
  await page.getByRole('button',{name:'Salir',exact:true}).click();await expect(page).toHaveURL(/\/acceso$/);
  await page.goto('/reservas');await expect(page).toHaveURL(/\/acceso$/);
});

test('recuperación elimina el token de la URL y lo envía únicamente en el cuerpo',async({page})=>{
  const token='A'.repeat(43);
  await page.route('**/api/auth/password-reset/confirm',async route=>{
    expect(route.request().postDataJSON()).toEqual({token,password:'nueva-clave-segura'});
    expect(route.request().url()).not.toContain(token);await route.fulfill({status:204});
  });
  await page.goto(`/recuperar#token=${token}`);
  await expect(page.getByLabel('Nueva contraseña')).toBeVisible();
  expect(new URL(page.url()).hash).toBe('');
  await page.getByLabel('Nueva contraseña').fill('nueva-clave-segura');await page.getByRole('button',{name:'Cambiar contraseña',exact:true}).click();
  await expect(page.getByText('Contraseña cambiada.',{exact:false})).toBeVisible();
});

test('el pago no se aprueba por parámetros de retorno y rechaza enlaces productivos',async({page})=>{
  await mockSession(page,{email:'ana@ejemplo.com',role:'TOURIST',touristId:7,agencyId:null,guideId:null});
  await page.route('**/api/payments/config',route=>route.fulfill({json:{enabled:true,sandbox:true}}));
  await page.route('**/api/reservations/42',route=>route.fulfill({json:{id:42,estado:'CONFIRMED',precioTotal:180000}}));
  let approved=false;
  await page.route('**/api/reservations/42/payment',route=>route.fulfill({json:{id:1,reservationId:42,amount:180000,currency:'COP',status:approved?'APPROVED':'PENDING',checkoutUrl:'https://www.mercadopago.com.co/checkout',sandbox:true}}));
  await page.route('**/api/reservations/42/payment/reconcile',async route=>{approved=true;await route.fulfill({json:{status:'APPROVED'}});});
  await page.goto('/acceso');await page.getByLabel('Correo electrónico').fill('ana@ejemplo.com');await page.getByLabel('Contraseña',{exact:true}).fill('una-clave-segura');await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
  await expect(page).toHaveURL(/\/reservas$/);await page.goto('/pagos?reservaId=42&status=approved');
  await expect(page.getByRole('heading',{name:'Pendiente de pago',exact:true})).toBeVisible();
  await expect(page.getByRole('link',{name:/Abrir Mercado Pago/})).toHaveCount(0);
  await page.getByRole('button',{name:'Verificar con el proveedor'}).click();await expect(page.getByRole('heading',{name:'Pago aprobado',exact:true})).toBeVisible();
});
