N = 1000;
S1 = sparse(1:N-1,2:N,1,N,N);
S2 = sparse(2:N,1:N-1,1,N,N);
A = S1+S2;
[row col v] = find (A);
dlmwrite('A.txt',[col row v],'delimiter',' ','precision', '%d');
%Dlmwrite ('myfile1.txt', pi, 'delimiter', ' ', 'precision', 16);

S3 = sparse(1:N,1:N,2,N,N);
A2 = S3-S1-S2;
[row2 col2 v2] = find (A2);
dlmwrite('A2.txt',[col2 row2 v2],'delimiter',' ','precision', '%d');

u = zeros(N,2);
for i = 1:N
    u(i,1)=i;
end
dlmwrite('u.txt', u ,'delimiter',' ','precision', '%d');

%u(:,[1,N]) = i