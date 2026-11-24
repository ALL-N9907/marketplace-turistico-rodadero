import { test, expect } from '@playwright/test';
// Dobles HTTP sólo en pruebas: nunca se incluyen en la aplicación.
const tour={id:1,agenciaId:1,tipoExperienciaId:1,titulo:'Caminata a Minca',descripcion:'Una caminata entre cafetales.',destino:'Minca',precio:180000,duracionHoras:8,nivelDificultad:'MEDIO',serviciosIncluidos:'Guía',estado:'ACTIVE'};
test.beforeEach(async({page})=>{
  await page.route('**/api/**',async route=>{
    const u=new URL(route.request().url()),p=u.pathname;
    let data:unknown=[];
    if(p==='/api/catalog/tours')data={content:[{tour,details:{supportsCommunities:true,environmentalPractices:false}}],totalElements:1,page:0,size:9};
    else if(p==='/api/agencies')data=[{id:1,nombreComercial:'Sierra Viva',estado:'ACTIVE'}];
    else if(p==='/api/experience-types')data=[{id:1,nombre:'Naturaleza'}];
    else if(p==='/api/tours/1')data=tour;
    else if(p==='/api/tours/1/details')data={itinerary:'07:00 Salida',conditions:'Llevar agua',supportsCommunities:true,environmentalPractices:false};
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
  await page.route('**/api/auth/me',route=>route.fulfill({json:{email:'ana@ejemplo.com',role:'TOURIST',touristId:7,agencyId:null,guideId:null}}));
  await page.route('**/api/reservations',async route=>{
    expect(route.request().method()).toBe('POST');
    expect(route.request().headers()['x-csrf-token']).toBe('test-token');
    expect(route.request().postDataJSON()).toMatchObject({turistaId:7,horarioId:3,cantidadPersonas:1});
    expect(route.request().postDataJSON()).not.toHaveProperty('estado');
    await route.fulfill({status:201,json:reservation});
  });
  await page.route('**/api/reservations/42',route=>route.fulfill({json:reservation}));
  await page.route('**/api/reservations/42/voucher',route=>route.fulfill({json:{reservationId:42,tour:tour.titulo,agency:'Sierra Viva',status:'SCHEDULED'}}));
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
});

test('operador consulta por una salida y no por un listado global',async({page})=>{
  await page.route('**/api/auth/me',route=>route.fulfill({json:{email:'operador@ejemplo.com',role:'AGENCY_OPERATOR',agencyId:2,touristId:null,guideId:null}}));
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
